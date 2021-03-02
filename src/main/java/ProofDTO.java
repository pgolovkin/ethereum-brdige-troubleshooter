import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.List;

public class ProofDTO {
    @JsonProperty("amount")
    BigInteger amount;

    @JsonProperty("irohaTxHash")
    String irohaTxHash;

    @JsonProperty("to")
    String to;

    @JsonProperty("proofs")
    List<SignatureDTO> proofs;
}
