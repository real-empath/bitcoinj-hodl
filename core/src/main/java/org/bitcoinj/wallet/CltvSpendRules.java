package org.bitcoinj.wallet;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.HodlScriptUtil;
import org.bitcoinj.script.Script;

public final class CltvSpendRules {
    private CltvSpendRules() {}

    /** If tx spends any CLTV-locked outputs, set non-final sequence and raise lockTime as required. */
    public static void enforce(Transaction tx) {
        long requiredLock = 0;
        boolean hasCltv = false;

        for (TransactionInput in : tx.getInputs()) {
            TransactionOutput prev = in.getConnectedOutput();
            if (prev == null) continue; // ensure wallet connected outpoints
            Script spk = prev.getScriptPubKey();
            if (HodlScriptUtil.isHodlCltvScript(spk)) {
                hasCltv = true;
                long lock = HodlScriptUtil.extractCltvLock(spk);
                if (lock > requiredLock) requiredLock = lock;
                // sequence must be non-final for CLTV to be enforced
                in.setSequenceNumber(TransactionInput.NO_SEQUENCE - 1); // 0xFFFFFFFE
            }
        }
        if (hasCltv && tx.getLockTime() < requiredLock) {
            tx.setLockTime(requiredLock);
        }
    }
}
