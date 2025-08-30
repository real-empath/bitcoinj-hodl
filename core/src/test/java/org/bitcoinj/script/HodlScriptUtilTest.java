package org.bitcoinj.script;

import org.bitcoinj.core.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class HodlScriptUtilTest {
    final NetworkParameters params = MainNetParams.get(); // or your Hodl params

    @Test
    public void build_and_detect() {
        Address dest = LegacyAddress.fromKey(params, new ECKey());
        long lock = 600_000_000L; // unix time lock (>= 500M)
        Script spk = HodlScriptUtil.createCltvScriptForAddress(dest, lock);

        assertTrue(HodlScriptUtil.isHodlCltvScript(spk));
        assertEquals(lock, HodlScriptUtil.extractCltvLock(spk));
    }
}

