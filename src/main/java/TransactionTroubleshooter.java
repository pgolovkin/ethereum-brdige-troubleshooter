import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.web3j.tx.Contract.BIN_NOT_PROVIDED;

public class TransactionTroubleshooter {

    private static final String url = "https://mainnet.infura.io/v3/d7d58ad3ce5c48d2b68a25343cf4149f"; //URL to Ethereum node
    private static final String pathToProof = "proof.json"; //Path to proof in JSON format. Here Iroha proofs should be located
    private static final String contractAddress = "0xd1eeb2f30016fffd746233ee12c486e7ca8efef1"; //Contract address
    private static final String tokenAddress = "0xe88f8313e61a97cec1871ee37fbbe2a8bf3ed1e4"; //token address

    /**
     * Ethereum private key should be sent as a args[0]
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Web3jService service = new HttpService(url);
        Web3j web3j = Web3j.build(service);


        ObjectMapper objectMapper = new ObjectMapper();
        ProofDTO proofs = objectMapper.readValue(TransactionTroubleshooter.class.getClassLoader().getResourceAsStream(pathToProof), ProofDTO.class);

        List<Uint8> v = new ArrayList<>();
        List<Bytes32> r = new ArrayList<>();
        List<Bytes32> s = new ArrayList<>();
        for (ProofDTO.SignatureDTO signature : proofs.signatures) {
            v.add(new Uint8(new BigInteger(signature.v, 16)));
            r.add(new Bytes32(DatatypeConverter.parseHexBinary(signature.r)));
            s.add(new Bytes32(DatatypeConverter.parseHexBinary(signature.s)));
        }
        Credentials credentials = Credentials.create(ECKeyPair.create(new BigInteger(args[0], 16)));

        BigDecimal amount = new BigDecimal("288.23");
        BigInteger amountWei = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
        Function function = new Function(
                "mintTokensByPeers",
                Arrays.asList(
                        new Address(tokenAddress),
                        new Uint256(amountWei),
                        new Address("0xdB7f0138F25bF1108506c836dff9F679CFd5317f"), //dest address
                        new Bytes32(Hex.decode("0c05f244173e5faec39a2345fc8a70a5df2bfc001e9d0f26fb76e6caeaf7fe51")), //IRoha transactions
                        new DynamicArray(v),
                        new DynamicArray(r),
                        new DynamicArray(s),
                        new Address("0xdB7f0138F25bF1108506c836dff9F679CFd5317f")), //dest address
                Collections.emptyList()
        );

        TransactionManager transactionManager = new TransactionManager(
                BIN_NOT_PROVIDED,
                contractAddress,
                web3j,
                credentials,
                new NonStandardDefaultGasProvider()
        );
        RemoteCall<TransactionReceipt> transaction = transactionManager.activate(function);
        String hash = transaction.send().getTransactionHash();
        System.out.println(hash);
    }

    private static class TransactionManager extends Contract {


        public TransactionManager(String contractBinary, String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
            super(contractBinary, contractAddress, web3j, credentials, gasProvider);
        }

        public RemoteCall<TransactionReceipt> activate(Function function) {
            return executeRemoteCallTransaction(function);
        }
    }

    private static class ProofDTO {

        @JsonProperty("status")
        StatusDTO status;

        @JsonProperty("hash")
        String hash;

        @JsonProperty("signatures")
        List<SignatureDTO> signatures;

        private static class StatusDTO {

            @JsonProperty("code")
            String code;

            @JsonProperty("message")
            String message;
        }

        private static class SignatureDTO {

            @JsonProperty("v")
            String v;

            @JsonProperty("r")
            String r;

            @JsonProperty("s")
            String s;
        }
    }
}