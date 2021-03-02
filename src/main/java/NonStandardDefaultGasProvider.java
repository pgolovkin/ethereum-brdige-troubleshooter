import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

public class NonStandardDefaultGasProvider extends StaticGasProvider {
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(350_000);

    public NonStandardDefaultGasProvider(BigInteger gasPrice) {
        super(gasPrice, GAS_LIMIT);
    }
}
