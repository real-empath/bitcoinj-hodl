package org.bitcoinj.wallet;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.CoinSelection;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.HodlScriptUtil;

import java.util.ArrayList;
import java.util.List;

public final class CltvAwareCoinSelector implements CoinSelector {
    private final BlockChain chain;
    private static final long LOCKTIME_THRESHOLD = 500_000_000L;

    public CltvAwareCoinSelector(BlockChain chain) {
        this.chain = chain;
    }

    @Override
    public CoinSelection select(Coin target, List<TransactionOutput> candidates) {
        int height = chain.getBestChainHeight();
        long time  = chain.getChainHead().getHeader().getTimeSeconds();

        List<TransactionOutput> ok = new ArrayList<>();
        for (TransactionOutput out : candidates) {
            try {
                if (HodlScriptUtil.isHodlCltvScript(out.getScriptPubKey())) {
                    long lock = HodlScriptUtil.extractCltvLock(out.getScriptPubKey());
                    boolean matured = (lock < LOCKTIME_THRESHOLD) ? height >= lock : time >= lock;
                    if (!matured) continue; // skip locked deposits until maturity
                }
                if (out.isAvailableForSpending()) ok.add(out);
            } catch (Exception ignore) { /* ignore malformed scripts */ }
        }
        return new DefaultCoinSelector().select(target, ok);
    }
}
