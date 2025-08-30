package org.bitcoinj.wallet;

import org.bitcoinj.core.*;
import org.bitcoinj.script.HodlScriptUtil;
import org.bitcoinj.script.Script;
import org.junit.Test;

import static org.junit.Assert.*;

public class CltvSpendRulesTest {
    final NetworkParameters params = MainNetParams.get(); // or Hodl

    @Test
    public void enforces_lock_and_sequence() throws Exception {
        // Build a fake prevout that is CLTV-locked
        Address dest = LegacyAddress.fromKey(params, new ECKey());
        long heightLock = 150000; // < 500M => height
        Script spk = HodlScriptUtil.createCltvScriptForAddress(dest, heightLock);

        Transaction prevTx = new Transaction(params);
        prevTx.addOutput(Coin.COIN, spk);

        // Spend it
        Transaction spend = new Transaction(params);
        spend.addInput(prevTx.getOutput(0));
        spend.addOutput(Coin.valueOf(50_0000), dest); // arbitrary

        // Enforce rules
        CltvSpendRules.enforce(spend);

        assertTrue("lockTime should be raised to required height", spend.getLockTime() >= heightLock);
        assertNotEquals("sequence must be non-final", TransactionInput.NO_SEQUENCE, spend.getInput(0).getSequenceNumber());
    }
}
