package garden.druid.pool.puzzles;

import java.math.BigInteger;

import garden.druid.chia.clvm.runtime.Pair;
import garden.druid.chia.clvm.runtime.Parser;
import garden.druid.chia.clvm.runtime.Result;
import garden.druid.chia.clvm.runtime.Runtime;
import garden.druid.chia.clvm.runtime.SExp;
import garden.druid.chia.types.bytes.Bytes;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;

public class LauncherIdToP2PuzzleHash {
	
	private static BigInteger NO_MAX = BigInteger.ONE.negate();
	private static final SExp puzzle = Parser.parse(Bytes.parseHexBinary("ff02ffff01ff02ffff03ff82017fffff01ff04ffff04ff38ffff04ffff0bffff02ff2effff04ff02ffff04ff05ffff04ff81bfffff04ffff02ff3effff04ff02ffff04ffff04ff05ffff04ff0bff178080ff80808080ff808080808080ff82017f80ff808080ffff04ffff04ff3cffff01ff248080ffff04ffff04ff28ffff04ff82017fff808080ff80808080ffff01ff04ffff04ff24ffff04ff2fff808080ffff04ffff04ff2cffff04ff5fffff04ff81bfff80808080ffff04ffff04ff10ffff04ff81bfff808080ff8080808080ff0180ffff04ffff01ffffff49ff463fffff5002ff333cffff04ff0101ffff02ff02ffff03ff05ffff01ff02ff36ffff04ff02ffff04ff0dffff04ffff0bff26ffff0bff2aff1280ffff0bff26ffff0bff26ffff0bff2aff3a80ff0980ffff0bff26ff0bffff0bff2aff8080808080ff8080808080ffff010b80ff0180ffff0bff26ffff0bff2aff3480ffff0bff26ffff0bff26ffff0bff2aff3a80ff0580ffff0bff26ffff02ff36ffff04ff02ffff04ff07ffff04ffff0bff2aff2a80ff8080808080ffff0bff2aff8080808080ff02ffff03ffff07ff0580ffff01ff0bffff0102ffff02ff3effff04ff02ffff04ff09ff80808080ffff02ff3effff04ff02ffff04ff0dff8080808080ffff01ff0bffff0101ff058080ff0180ff018080"));
	
	public static Bytes32 runProgram(Bytes32 launcher_id, NativeUInt64 delay_time, Bytes32 delay_puzzle_hash) {

		SExp args = createArgs(
				new SExp(launcher_id.getBytes()), 
				new SExp(delay_time.toBigInteger()), 
				new SExp(delay_puzzle_hash.getBytes()));
		Runtime runtime = new Runtime(puzzle, args, NO_MAX);
		Result result = runtime.run();
		byte[] ary = result.getResult().getAtom().getBytes();
		return new Bytes32(ary);
	}
	
	public static SExp createArgs(SExp... args) {
		SExp rtn = new SExp(new Pair(args[args.length-1], null));
		for(int i = args.length-2; i >= 0 ; i--) {
			rtn = new SExp(new Pair(args[i], rtn));
		}
		return rtn;
	}
}
