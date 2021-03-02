import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ResponseDTO {
    @JsonProperty("status")
    StatusDTO status;

    @JsonProperty("proofs")
    List<ProofDTO> proofs;


    private static class StatusDTO {

        @JsonProperty("code")
        String code;

        @JsonProperty("message")
        String message;
    }
}
