package garden.druid.pool.puzzles;

import garden.druid.base.logging.Logger;
import garden.druid.chia.types.blockchain.Coin;
import garden.druid.chia.types.blockchain.CoinSpend;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.types.PoolState;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;

import com.google.gson.Gson;

public class PoolPuzzles {

    private static final Gson gson = new Gson();

    private static final String SCRIPT_PATH = "/home/tomcat/";
    private static final String SCRIPT_NAME = "chia_utils.py";

    public static PoolState solutionToPoolState(CoinSpend last_solution) {
        try {
            if (last_solution == null) {
                return null;
            }
            PoolState rtn;
            StringBuilder rtnValue = new StringBuilder();
            Process p = null;
            BufferedReader br = null;
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", SCRIPT_NAME, "solution_to_pool_state", gson.toJson(last_solution));
                pb.directory(new File(SCRIPT_PATH));
                p = pb.start();
                p.waitFor();
                String tmp;
                if (p.exitValue() > 0) {
                    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    while ((tmp = br.readLine()) != null) {
                        Logger.getInstance().log(Level.WARNING, tmp);
                    }
                } else {
                    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((tmp = br.readLine()) != null) {
                        rtnValue.append(tmp);
                        rtnValue.append(System.lineSeparator());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (p != null && p.isAlive()) {
                    p.destroy();
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            rtn = gson.fromJson(rtnValue.toString(), PoolState.class);
            return rtn;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean validate_puzzle_hash(PoolState pool_state, Bytes32 launcher_id, Bytes32 delay_ph, NativeUInt64 delay_time, Bytes32 outer_puzzle_hash, Bytes32 genesis_challenge) {
        try {
            if (pool_state == null || launcher_id == null || genesis_challenge == null || delay_time == null || delay_ph == null) {
                return false;
            }
            StringBuilder rtnValue = new StringBuilder();
            Process p = null;
            BufferedReader br = null;
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", SCRIPT_NAME, "validate_puzzle_hash", gson.toJson(pool_state), launcher_id.toString(), genesis_challenge.toString(), delay_time.toString(), delay_ph.toString(), outer_puzzle_hash.toString());
                pb.directory(new File(SCRIPT_PATH));
                p = pb.start();
                p.waitFor();
                String tmp;
                if (p.exitValue() > 0) {
                    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    while ((tmp = br.readLine()) != null) {
                        Logger.getInstance().log(Level.SEVERE, tmp);
                    }
                } else {
                    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((tmp = br.readLine()) != null) {
                        rtnValue.append(tmp);
                    }
                }
                return Boolean.parseBoolean(rtnValue.toString());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (p != null && p.isAlive()) {
                    p.destroy();
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static CoinSpend[] create_absorb_spend(CoinSpend last_spend, PoolState state, Coin coin, NativeUInt32 found_block_index, Bytes32 genesis_challenge, NativeUInt64 delay_time, Bytes32 delay_puzzle_hash) {
        CoinSpend[] rtn = null;
        try {
            if (last_spend == null || state == null || coin == null || found_block_index == null || genesis_challenge == null || delay_time == null || delay_puzzle_hash == null) {
                return null;
            }
            StringBuilder rtnValue = new StringBuilder();
            Process p = null;
            BufferedReader br = null;
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", SCRIPT_NAME, "create_absorb_spend", gson.toJson(last_spend), gson.toJson(state), gson.toJson(coin), found_block_index.toString(), genesis_challenge.toString(), delay_time.toString(), delay_puzzle_hash.toString());
                pb.directory(new File(SCRIPT_PATH));
                p = pb.start();
                p.waitFor();
                String tmp;
                if (p.exitValue() > 0) {
                    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    while ((tmp = br.readLine()) != null) {
                        Logger.getInstance().log(Level.WARNING, tmp);
                    }
                } else {
                    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((tmp = br.readLine()) != null) {
                        rtnValue.append(tmp);
                        rtnValue.append(System.lineSeparator());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (p != null && p.isAlive()) {
                    p.destroy();
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            rtn = gson.fromJson(rtnValue.toString(), CoinSpend[].class);
            return rtn;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
