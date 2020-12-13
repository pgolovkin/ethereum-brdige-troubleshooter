import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

public class NonStandardDefaultGasProvider extends StaticGasProvider {
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(350_000);
    public static final BigInteger GAS_PRICE = BigInteger.valueOf(60_000_000_000L);

    public NonStandardDefaultGasProvider() {
        super(GAS_PRICE, GAS_LIMIT);
    }
}