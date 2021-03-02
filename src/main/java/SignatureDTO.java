import com.fasterxml.jackson.annotation.JsonProperty;

public class SignatureDTO {
    @JsonProperty("v")
    String v;

    @JsonProperty("r")
    String r;

    @JsonProperty("s")
    String s;
}
