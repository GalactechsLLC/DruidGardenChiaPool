import sys
import json
from typing import List
from chia.util.ints import uint8, uint16, uint32, uint64
from chia.types.blockchain_format.sized_bytes import bytes32
from chia.types.blockchain_format.coin import Coin
from chia.types.blockchain_format.program import Program
from chia.types.coin_spend import CoinSpend
from chia.pools.pool_wallet_info import PoolState
from chia.pools.pool_puzzles import (
	create_absorb_spend,
	get_most_recent_singleton_coin_from_coin_spend,
	get_delayed_puz_info_from_launcher_spend,
	launcher_id_to_p2_puzzle_hash,
	solution_to_pool_state,
	pool_state_to_inner_puzzle,
	create_full_puzzle
)
from chia import __version__
from chiapos import Verifier

def hexstr_to_bytes(input_str: str) -> bytes:
	if input_str.startswith("0x") or input_str.startswith("0X"):
		return bytes.fromhex(input_str[2:])
	return bytes.fromhex(input_str)

def validate_proof(plot_id: str, size: int, challenge: str, proof: str) -> str:
	quality_str = Verifier().validate_proof(bytes32(hexstr_to_bytes(plot_id)), uint8(size), bytes32(hexstr_to_bytes(challenge)), hexstr_to_bytes(proof))
	if not quality_str:
		return None
	quality_str_bytes: bytes = bytes(bytes32(quality_str))
	return quality_str_bytes.hex()

def __get_delayed_puz_info_from_launcher_spend__(coinSpendJson: str) -> str:
	launcher_solution: CoinSpend = CoinSpend.from_json_dict(json.loads(coinSpendJson))
	delay_time, delay_puzzle_hash = get_delayed_puz_info_from_launcher_spend(launcher_solution)
	return json.dumps(dict([("delay_time", delay_time),("delay_puzzle_hash", delay_puzzle_hash.hex())]))

def __launcher_id_to_p2_puzzle_hash__(launcher_id: str, delay_time: int, delay_puzzle_hash: str) -> str:
	p2_singleton_puzzle_hash = launcher_id_to_p2_puzzle_hash(bytes32(hexstr_to_bytes(launcher_id)), delay_time, bytes32(hexstr_to_bytes(delay_puzzle_hash)))
	p2_singleton_puzzle_hash_bytes: bytes = bytes(p2_singleton_puzzle_hash)
	return p2_singleton_puzzle_hash_bytes.hex()

def __get_most_recent_singleton_coin_from_coin_spend__(last_solution: str) -> str:
	olast_solution: CoinSpend = CoinSpend.from_json_dict(json.loads(last_solution))
	singleton_tip: Optional[Coin] = get_most_recent_singleton_coin_from_coin_spend(olast_solution)
	if singleton_tip is not None:
		return singleton_tip.to_json_dict()
	else: 
		return "null"
	
def __solution_to_pool_state__(last_solution: str) -> str:
	olast_solution: CoinSpend = CoinSpend.from_json_dict(json.loads(last_solution))
	saved_state: Optional[PoolState] = solution_to_pool_state(olast_solution)
	if saved_state is not None:
		return saved_state.to_json_dict()
	else: 
		return "null"

def validate_puzzle_hash(pool_state: str, launcher_id: str, genesis_challenge: str, delay_time: int, delay_ph: str, outer_puzzle_hash: str) -> str:
	oPool_state: PoolState = PoolState.from_json_dict(json.loads(pool_state))
	inner_puzzle: Program = pool_state_to_inner_puzzle(oPool_state, bytes32(hexstr_to_bytes(launcher_id)), bytes32(hexstr_to_bytes(genesis_challenge)), int(delay_time), bytes32(hexstr_to_bytes(delay_ph)))
	new_full_puzzle: Program = create_full_puzzle(inner_puzzle, bytes32(hexstr_to_bytes(launcher_id)))	
	if new_full_puzzle.get_tree_hash() == bytes32(hexstr_to_bytes(outer_puzzle_hash)):
		return "true"
	else: 
		return "false"

def __create_absorb_spend__(last_spend: str, state: str, coin: str, found_block_index: str, genesis_challenge: str, delay_time: int, delay_ph: str) -> str:
	oLast_spend = CoinSpend.from_json_dict(json.loads(last_spend))
	oState = PoolState.from_json_dict(json.loads(state))
	oCoin = Coin.from_json_dict(json.loads(coin))
	ifound_block_index = uint32(int(found_block_index))
	bgenesis_challenge = bytes32(hexstr_to_bytes(genesis_challenge))
	idelay_time = uint64(int(delay_time))
	bdelay_ph = bytes32(hexstr_to_bytes(delay_ph))
	absorb_spend: List[CoinSpend] = create_absorb_spend(oLast_spend, oState, oCoin, ifound_block_index, bgenesis_challenge, idelay_time, bdelay_ph)
	if absorb_spend is not None:
		rtnList: List[str] = []
		for coin_spend in absorb_spend:
			rtnList.append(coin_spend.to_json_dict())
		return json.dumps(rtnList)
	else: 
		return "null"

def printVersion():
	print(__version__)

def __main__():
	sys.argv[0]
	if len(sys.argv) > 1:
		command = sys.argv[1]
		if command == "validate_proof":
			plot_id = sys.argv[2]
			size = sys.argv[3]
			challenge = sys.argv[4]
			proof = sys.argv[5]
			print(validate_proof(plot_id, size, challenge, proof))
		elif command == "get_delayed_puz_info_from_launcher_spend":
			coinSpendJson = sys.argv[2]
			print(__get_delayed_puz_info_from_launcher_spend__(coinSpendJson))
		elif command == "launcher_id_to_p2_puzzle_hash":
			launcher_id = sys.argv[2]
			delay_time = sys.argv[3]
			delay_puzzle_hash = sys.argv[4]
			print(__launcher_id_to_p2_puzzle_hash__(launcher_id, int(delay_time), delay_puzzle_hash))
		elif command == "get_most_recent_singleton_coin_from_coin_spend":
			last_solution = sys.argv[2]
			print(__get_most_recent_singleton_coin_from_coin_spend__(last_solution))
		elif command == "solution_to_pool_state":
			last_solution = sys.argv[2]
			print(__solution_to_pool_state__(last_solution))
		elif command == "validate_puzzle_hash":
			pool_state = sys.argv[2]
			launcher_id = sys.argv[3]
			genesis_challenge = sys.argv[4]
			delay_time = sys.argv[5]
			delay_ph = sys.argv[6]
			outer_puzzle_hash = sys.argv[7]
			print(validate_puzzle_hash(pool_state, launcher_id, genesis_challenge, delay_time, delay_ph, outer_puzzle_hash))
		elif command == "create_absorb_spend":
			last_spend = sys.argv[2]
			state = sys.argv[3]
			coin = sys.argv[4]
			found_block_index = sys.argv[5]
			genesis_challenge = sys.argv[6]
			delay_time = sys.argv[7]
			delay_puzzle_hash = sys.argv[8]
			print(__create_absorb_spend__(last_spend, state, coin, found_block_index, genesis_challenge, delay_time, delay_puzzle_hash))
		elif command == "version":
			printVersion()
		else:
			print("invalid command")

__main__();
