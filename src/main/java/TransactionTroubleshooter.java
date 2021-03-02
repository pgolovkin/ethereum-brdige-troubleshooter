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
import java.util.stream.Collectors;

import static org.web3j.tx.Contract.BIN_NOT_PROVIDED;

public class TransactionTroubleshooter {

    private static final String url = "https://mainnet.infura.io/v3/d7d58ad3ce5c48d2b68a25343cf4149f"; //URL to Ethereum node
    private static final String pathToProof = "proof.json"; //Path to proof in JSON format. Here Iroha proofs should be located
    private static final String contractAddress = "0xd1eeb2f30016fffd746233ee12c486e7ca8efef1"; //Contract address
    private static final String tokenAddress = "0xe88f8313e61a97cec1871ee37fbbe2a8bf3ed1e4"; //token address

    private static final List<String> transactionHashes = Arrays.asList("6895a7f3354edf81422fc553668bd7b4315732683f03cd75ded8df6264dad2d0",
            "6c3b491a6234fca1ce536eeb3bfa79516050fbd9abacba0be621ff63ff12511a",
            "386a26dca990191be73fddd8a280c024ad448856b922e30bc26f3f3170f13e85",
            "4ba6b4b7458f23f87c41a354db508f3d7afcecd96ce0ad33408f08e8cfae5a98",
            "79f58842291ece0d4a25ce70f354e70f6ccb4b0f1cb17b0ea1667ed79e48731c",
            "b7f9e6639551bcbc6b6deee4bce110b05ff4e3bf543b14c83795aacb9d9320b0",
            "82b670e8b50ce56b0ecb31a53897a1196fdcc11df6a98742e340e91ad5e78e34",
            "8d5f719e67f3bda8adf93106b0a30a5774646cfcf6dd01dae99f836dbe4061f0",
            "060e05b3d4a8e3b4bfde2a90628a22f1bb02d9a5317d7f1d473ec22a63bb8479",
            "a88811b2050281a538d33da2a94148752b8516345af4b4915fea80f6134ad760",
            "b7a642723d0d5c3f6d75645f8a5d839696e0561493d610e3da6de99be8d9cf96");

    private static final String did = "did_sora_f65f6add503d4c535f6d@sora";

    /**
     * Ethereum private key should be sent as a args[0]
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://sora-events.s1.soranet.soramitsu.co.jp/v1/notification/find/withdrawalProofs/" + did)
                .build();

        ResponseDTO response = null;
        try (Response r = client.newCall(request).execute()) {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            response = objectMapper.readValue(r.body().string(), ResponseDTO.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }


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
