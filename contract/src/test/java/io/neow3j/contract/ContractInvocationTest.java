package io.neow3j.contract;

import io.neow3j.contract.ContractInvocation.Builder;
import io.neow3j.crypto.transaction.RawTransactionAttribute;
import io.neow3j.crypto.transaction.RawTransactionOutput;
import io.neow3j.model.types.GASAsset;
import io.neow3j.model.types.NEOAsset;
import io.neow3j.model.types.TransactionAttributeUsageType;
import io.neow3j.protocol.Neow3j;
import io.neow3j.transaction.InvocationTransaction;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.InputCalculationStrategy;
import io.neow3j.wallet.Utxo;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class ContractInvocationTest {

    /**
     * Account with address AK2nJJpJr6o664CWJKi1QRXjqeic2zRp8y
     */
    private static final Account ACCT = Account.fromWIF("KxDgvEKzgSBPPfuVfw67oPQBSjidEiqTHURKSDL1R7yGaGYAeYnr").build();

    /**
     * An empty Neow3j instance for all tests which don't actually need to make the final invocation
     * call.
     */
    private static final Neow3j EMPTY_NEOW3J = Neow3j.build(null);

    /**
     * Script hash from name service smart contract (NS SC) at
     * https://github.com/CityOfZion/python-smart-contract-workshop/blob/master/4-domain.py.
     * Compiled with Neo EcoLab compiler.
     */
    private static final String NS_SC_SCRIPT_HASH = "1a70eac53f5882e40dd90f55463cce31a9f72cd4";

    /**
     * First parameter to the name service smart contract, used for registering a name.
     */
    private static final ContractParameter REGISTER = ContractParameter.string("register");

    /**
     * Array parameter needed when registering a name with the name service smart contract. Uses the
     * address of the {@link ContractInvocationTest#ACCT} as the owner of the name "neo.com".
     */
    private static final ContractParameter ARGUMENTS = ContractParameter.array(
            ContractParameter.string("neo.com"),
            ContractParameter.byteArrayFromAddress(ACCT.getAddress()));

    /**
     * This tests the raw transaction array built by the ContractInvocation for a invocation made
     * to the name service smart contract with the 'register' operation. The transaction was
     * executed on the Neo EcoLab private network. The only GAS input that was used for paying the
     * network fee is mocked in the test. <br><br>
     *
     * The EcoLab always attaches an attribute to the transaction that holds the script hash of the
     * invoking account. We only do this in case the inputs and outputs of the transaction are
     * empty, i.e. if no fee is attached. Because this is not the case in this test, the script hash
     * attribute is added manually in the builder.
     */
    @Test
    public void invocation_with_network_fee() {
        BigDecimal fee = BigDecimal.ONE;
        Account spyAcct = spy(ACCT);
        Utxo utxo = new Utxo("9f1b9a6f3593ff546a9dab147ba8ad520f7b6233bb0f8e75e05ad23d57ebd76e", 0, BigDecimal.valueOf(96));
        doReturn(Arrays.asList(utxo)).when(spyAcct).getUtxosForAssetAmount(GASAsset.HASH_ID, fee, InputCalculationStrategy.DEFAULT_INPUT_CALCULATION_STRATEGY);

        InvocationTransaction tx = new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(NS_SC_SCRIPT_HASH)
                .account(spyAcct)
                .networkFee(fee)
                .parameter(REGISTER)
                .parameter(ARGUMENTS)
                .attribute(new RawTransactionAttribute(TransactionAttributeUsageType.SCRIPT, spyAcct.getScriptHash()))
                .build()
                .sign()
                .getTransaction();

        String txHex = Numeric.toHexStringNoPrefix(tx.toArray());
        // Transaction hex string produced from a valid transaction executed on the EcoLab private net.
        String expextedTxHex = "d1013d1423ba2703c53263e8d6e522dc32203339dcd8eee9076e656f2e636f6d52c108726567697374657" +
                "267d42cf7a931ce3c46550fd90de482583fc5ea701a0000000000000000012023ba2703c53263e8d6e522dc32203339dcd8e" +
                "ee9016ed7eb573dd25ae0758e0fbb33627b0f52ada87b14ab9d6a54ff93356f9a1b9f000001e72d286979ee6cb1b7e65dfdd" +
                "fb2e384100b8d148e7758de42e4168b71792c60007f3e360200000023ba2703c53263e8d6e522dc32203339dcd8eee901414" +
                "037298f37fa7360a36cfec1afabcadd30cc69bfd1dfb3f396c25515d5f6831a875c6eb26928706c19f796333ddfce26f3fc8" +
                "fd2ce0942753556153118c54a82f52321031a6c6fbbdf02ca351745fa86b9ba5a9452d785ac4f7fc2b7548ca2a46c4fcf4aac";
        assertEquals(expextedTxHex, txHex);
    }

    /**
     * This tests the raw transaction array built by the ContractInvocation for an invocation made
     * to the name service smart contract with the 'register' operation. The transaction was
     * executed on the Neo EcoLab private network. <br><br>
     *
     * No fees are attached and therefore no inputs or outputs required. Therefore it is necessary
     * to attach a random remark which is usually based on some randomness and the current time.
     * This value was extracted from the transaction executed on the private net to mock the random
     * remark.
     */
    @Test
    public void invocation_without_fee() {
        Builder builder = new ContractInvocation.Builder(EMPTY_NEOW3J);
        builder = spy(builder);
        byte[] randomRemark = Numeric.hexStringToByteArray("313536333335343634353935313136343034643835");
        doReturn(randomRemark).when(builder).createRandomRemark();

        InvocationTransaction tx = builder.contractScriptHash(NS_SC_SCRIPT_HASH)
                .account(ACCT)
                .parameter(REGISTER)
                .parameter(ARGUMENTS)
                .build()
                .sign()
                .getTransaction();

        String txHex = Numeric.toHexStringNoPrefix(tx.toArray());
        // Transaction hex string produced from a valid transaction executed on the EcoLab private net.
        String expextedTxHex = "d1013d1423ba2703c53263e8d6e522dc32203339dcd8eee9076e656f2e636f6d52c108726567697374657" +
                "267d42cf7a931ce3c46550fd90de482583fc5ea701a0000000000000000022023ba2703c53263e8d6e522dc32203339dcd8e" +
                "ee9f0153135363333353436343539353131363430346438350000014140ae90f2c650ba69d1a90c3c5d915b07613e32f98c2" +
                "5de139b0be8f6977d4d0ecd86ef482f7e6d97a1ba64f6b03292a617e87a77674817cf156795fa26515793302321031a6c6fb" +
                "bdf02ca351745fa86b9ba5a9452d785ac4f7fc2b7548ca2a46c4fcf4aac";
        assertEquals(expextedTxHex, txHex);
    }

    /**
     * This tests the raw transaction array built by the ContractInvocation for an invocation made
     * to the name service smart contract with the 'register' operation. The transaction was
     * executed on the Neo EcoLab private network. <br><br>
     *
     * This time the transaction was extended with a NEO output to some other address then the one
     * doing the invocation. The required inputs for that output where mocked according to the Utxo
     * that was available when running the transaction on the EcoLab private net.
     *
     * The EcoLab always attaches an attribute to the transaction that holds the script hash of the
     * invoking account. Therefore, in this test, the script hash attribute is added manually in the
     * builder.
     */
    @Test
    public void invocation_with_additional_outputs_no_fee() {
        String toAddress = "Ab7kmZJw2yJDNREnyBByt1QEZGbzj9uBf1";
        BigDecimal neoOut = BigDecimal.ONE;

        Account spyAcct = spy(ACCT);
        Utxo utxo = new Utxo("f37ad4968c0554e9c4b2ccc943b917e5f2e04a85254fc650ef3d9b3fe3c74105", 1, BigDecimal.valueOf(99999999));
        doReturn(Arrays.asList(utxo)).when(spyAcct).getUtxosForAssetAmount(NEOAsset.HASH_ID, neoOut, InputCalculationStrategy.DEFAULT_INPUT_CALCULATION_STRATEGY);

        InvocationTransaction tx = new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(NS_SC_SCRIPT_HASH)
                .account(spyAcct)
                .parameter(REGISTER)
                .parameter(ARGUMENTS)
                .attribute(new RawTransactionAttribute(TransactionAttributeUsageType.SCRIPT, spyAcct.getScriptHash()))
                .output(RawTransactionOutput.createNeoTransactionOutput(neoOut.toPlainString(), toAddress))
                .build()
                .sign()
                .getTransaction();

        String txHex = Numeric.toHexStringNoPrefix(tx.toArray());
        // Transaction hex string produced from a valid transaction executed on the EcoLab private net.
        String expextedTxHex = "d1013d1423ba2703c53263e8d6e522dc32203339dcd8eee9076e656f2e636f6d52c108726567697374657" +
                "267d42cf7a931ce3c46550fd90de482583fc5ea701a0000000000000000012023ba2703c53263e8d6e522dc32203339dcd8e" +
                "ee9010541c7e33f9b3def50c64f25854ae0f2e517b943c9ccb2c4e954058c96d47af30100029b7cffdaa674beae0f930ebe6" +
                "085af9093e5fe56b34a5c220ccdcf6efc336fc500e1f50500000000d42cf7a931ce3c46550fd90de482583fc5ea701a9b7cf" +
                "fdaa674beae0f930ebe6085af9093e5fe56b34a5c220ccdcf6efc336fc5003ed563f286230023ba2703c53263e8d6e522dc3" +
                "2203339dcd8eee90141400131b26785f2b522ea420e6f432611ffdb1bf0b2e1f7473eef17124faf227f27693bffef0765467" +
                "7025bae09b90e8e5a3c0918c35a3c8ec6ab97090541687bd22321031a6c6fbbdf02ca351745fa86b9ba5a9452d785ac4f7fc" +
                "2b7548ca2a46c4fcf4aac";
        assertEquals(expextedTxHex, txHex);
    }

    @Test
    public void random_remark() {
        ContractInvocation i = new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(NS_SC_SCRIPT_HASH)
                .account(ACCT)
                .parameter(REGISTER)
                .parameter(ARGUMENTS)
                .build();

        RawTransactionAttribute attr = i.getTransaction().getAttributes().get(0);
        assertEquals(TransactionAttributeUsageType.SCRIPT, attr.getUsage());
        assertArrayEquals(ACCT.getScriptHash(), attr.getDataAsBytes());

        attr = i.getTransaction().getAttributes().get(1);
        assertEquals(TransactionAttributeUsageType.REMARK, attr.getUsage());
        assertEquals(12, attr.getDataAsBytes().length);
        // Can't test the contents of the remark because they are random.
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_too_big_script_hash_bytes() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(Numeric.hexStringToByteArray(NS_SC_SCRIPT_HASH + "4"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_too_big_script_hash_string() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(NS_SC_SCRIPT_HASH + "4")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_too_small_script_hash_bytes() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(new byte[]{0x1a, 0x38, 0x3b, 0x0d})
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_too_small_script_hash_string() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash("a0c882d")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_empty_script_hash_string() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_empty_script_hash_bytes() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(new byte[]{})
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void not_adding_required_script_hash() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .account(ACCT)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void not_adding_required_account() {
        new ContractInvocation.Builder(EMPTY_NEOW3J)
                .contractScriptHash(NS_SC_SCRIPT_HASH)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void not_adding_rquired_neow3j() {
        new ContractInvocation.Builder(null)
                .account(ACCT)
                .contractScriptHash(NS_SC_SCRIPT_HASH)
                .build();
    }

}
