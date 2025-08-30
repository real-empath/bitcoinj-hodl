package org.bitcoinj.script;

import org.bitcoinj.core.Address;

import java.util.List;

public final class HodlScriptUtil {
    private HodlScriptUtil() {}

    /** Build: <lock> CLTV DROP  <standard script for dest> (P2PKH or P2SH) */
    public static Script createCltvScriptForAddress(Address dest, long lock) {
        Script std = ScriptBuilder.createOutputScript(dest);  // tail = P2PKH or P2SH
        ScriptBuilder sb = new ScriptBuilder()
                .number(lock)
                .op(ScriptOpCodes.OP_NOP2)
                .op(ScriptOpCodes.OP_DROP);
        for (ScriptChunk c : std.getChunks()) sb.addChunk(c);
        return sb.build();
    }

    /** True if: <num> CLTV DROP  (P2PKH|P2SH) tail */
    public static boolean isHodlCltvScript(Script s) {
        List<ScriptChunk> ch = s.getChunks();
        if (ch.size() < 4) return false;

        // First chunk must be a small-int opcode (OP_0..OP_16) or a minimally-encoded number push
        ScriptChunk c0 = ch.get(0);
        boolean firstIsNumber =
                (!c0.isOpCode()) ||
                c0.opcode == ScriptOpCodes.OP_0 ||
                (c0.opcode >= ScriptOpCodes.OP_1 && c0.opcode <= ScriptOpCodes.OP_16);
        if (!firstIsNumber) return false;

        if (ch.get(1).opcode != ScriptOpCodes.OP_NOP2) return false;
        if (ch.get(2).opcode != ScriptOpCodes.OP_DROP) return false;

        // Remainder should be a standard pay script (we expect P2PKH or P2SH here)
        Script tail = new Script(ch.subList(3, ch.size()));
        return ScriptPattern.isPayToPubKeyHash(tail) || ScriptPattern.isPayToScriptHash(tail);
    }

    /** Extract locktime: height if < 500_000_000; else UNIX time (per CLTV rules). */
    public static long extractCltvLock(Script s) {
        ScriptChunk c0 = s.getChunks().get(0);
        if (c0.isOpCode()) {
            // OP_0..OP_16
            return Script.decodeFromOpN(c0.opcode);
        }
        // minimally-encoded integer push
        return Script.castToBigInteger(c0.data, 5, false).longValue();
    }
}
