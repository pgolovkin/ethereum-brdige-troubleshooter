import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.web3j.tx.Contract.BIN_NOT_PROVIDED;

public class TransactionTroubleshooter {

    private static final String url = "https://mainnet.infura.io/v3/d7d58ad3ce5c48d2b68a25343cf4149f"; //URL to Ethereum node
    private static final String contractAddress = "0xd1eeb2f30016fffd746233ee12c486e7ca8efef1"; //Contract address
    private static final String tokenAddress = "0xe88f8313e61a97cec1871ee37fbbe2a8bf3ed1e4"; //token address

    /**
     * Ethereum private key should be sent as a args[0]
     * SORA did should be sent as an args[1]
     * Comma separated list of SORA transactions hashes should be sent as args[2]
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Wrong number of arguments");
            System.exit(0);
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://sora-events.s1.soranet.soramitsu.co.jp/v1/notification/find/withdrawalProofs/" + args[1])
                .build();

        ResponseDTO response;
        try (Response r = client.newCall(request).execute()) {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            response = objectMapper.readValue(r.body().string(), ResponseDTO.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }

        List<String> transactionHashes = Arrays.asList(args[2].split(","));
        response.proofs.stream()
                .filter(p -> transactionHashes.contains(p.irohaTxHash.toLowerCase()))
                .forEach(p -> {
                    try {
                        resendTransaction(p, args[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }

    static void resendTransaction(ProofDTO proofDTO, String ethereumKey) throws Exception {
        System.out.println("processing transaction " + proofDTO.irohaTxHash);
        Web3jService service = new HttpService(url);
        Web3j web3j = Web3j.build(service);
        List<Uint8> v = new ArrayList<>();
        List<Bytes32> r = new ArrayList<>();
        List<Bytes32> s = new ArrayList<>();
        for (SignatureDTO signature : proofDTO.proofs) {
            v.add(new Uint8(new BigInteger(signature.v, 16)));
            r.add(new Bytes32(DatatypeConverter.parseHexBinary(signature.r)));
            s.add(new Bytes32(DatatypeConverter.parseHexBinary(signature.s)));
        }
        Credentials credentials = Credentials.create(ECKeyPair.create(new BigInteger(ethereumKey, 16)));

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        Function function = new Function(
                "mintTokensByPeers",
                Arrays.asList(
                        new Address(tokenAddress),
                        new Uint256(proofDTO.amount),
                        new Address(proofDTO.to), //dest address
                        new Bytes32(Hex.decode(proofDTO.irohaTxHash)), //Iroha transactions
                        new DynamicArray(v),
                        new DynamicArray(r),
                        new DynamicArray(s),
                        new Address(proofDTO.to)), //dest address
                Collections.emptyList()
        );


        TransactionManager transactionManager = new TransactionManager(
                BIN_NOT_PROVIDED,
                contractAddress,
                web3j,
                credentials,
                new NonStandardDefaultGasProvider(gasPrice)
        );
        RemoteCall<TransactionReceipt> transaction = transactionManager.activate(function);
        String hash = transaction.send().getTransactionHash();
        System.out.println("irho tx " + proofDTO.irohaTxHash + " ethereum tx " + hash);
    }

    private static class TransactionManager extends Contract {


        public TransactionManager(String contractBinary, String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
            super(contractBinary, contractAddress, web3j, credentials, gasProvider);
        }

        public RemoteCall<TransactionReceipt> activate(Function function) {
            return executeRemoteCallTransaction(function);
        }
    }
}
