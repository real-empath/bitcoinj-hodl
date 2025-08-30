package org.bitcoinj.wallet;

import org.bitcoinj.core.*;
import org.bitcoinj.script.HodlScriptUtil;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class CltvAwareCoinSelectorTest {
    final NetworkParameters params = MainNetParams.get(); // or Hodl params

    /** Simple fake chain head; replace with your test chain util if available */
    private BlockChain fakeChain(int height, long time) {
        // If your tree has a TestBlockChain builder, use it. Otherwise, mock or wrap.
        return new BlockChain(params, new Wallet(params), new MemoryBlockStore(params)) {
            @Override public int getBestChainHeight() { return height; }
            @Override public StoredBlock getChainHead() {
                try {
                    Block b = new Block(params, Block.BLOCK_VERSION_GENESIS);
                    b.setTimeSeconds(time);
                    return new StoredBlock(b, b.getWork(), height);
                } catch (VerificationException e) { throw new RuntimeException(e); }
            }
        };
    }

    @Test
    public void skips_until_mature() throws Exception {
        ECKey k = new ECKey();
        Address dest = LegacyAddress.fromKey(params, k);

        long future = 700_000_000L; // unix time in the future
        Script cltv = HodlScriptUtil.createCltvScriptForAddress(dest, future);

        TransactionOutput out = new TransactionOutput(params, null, Coin.valueOf(10_0000), cltv.getProgram());
        CltvAwareCoinSelector sel = new CltvAwareCoinSelector(fakeChain(1000, 600_000_000L));

        CoinSelection cs = sel.select(Coin.valueOf(1_0000), Collections.singletonList(out));
        assertTrue("Should be empty before maturity", cs.gathered.isEmpty());

        // After time passes
        sel = new CltvAwareCoinSelector(fakeChain(1000, future));
        cs = sel.select(Coin.valueOf(1_0000), Collections.singletonList(out));
        assertFalse("Should be selectable after maturity", cs.gathered.isEmpty());
    }
}
