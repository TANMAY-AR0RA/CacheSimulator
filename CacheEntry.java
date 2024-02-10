import java.util.*;
import static java.util.Map.entry;
import java.math.*;
import java.io.*;

class Base_conversion {
	static Map<Character, String> Map_cnv = Map.ofEntries(
			entry('0', "0000"),
			entry('1', "0001"),
			entry('2', "0010"),
			entry('3', "0011"),
			entry('4', "0100"),
			entry('5', "0101"),
			entry('6', "0110"),
			entry('7', "0111"),
			entry('8', "1000"),
			entry('9', "1001"),
			entry('A', "1010"),
			entry('B', "1011"),
			entry('C', "1100"),
			entry('D', "1101"),
			entry('E', "1110"),
			entry('F', "1111")
	);

	static String hexToBin(String str)
	{
		str = str.toUpperCase();
		StringBuilder bin = new StringBuilder();
		for(int b =0; b<str.length();b+=1){
			bin.append(Map_cnv.get(str.charAt(b)));
		}
		return bin.toString();
	}
}

class Node{
	String nodeStr;
	int nodeIndex;
	public Node(String NodeStr, int NodeIndex) {
		super();
		this.nodeStr = NodeStr;
		this.nodeIndex = NodeIndex;
	}
	public String getNodeStr() {
		return nodeStr;
	}
	public int getNodeIndex() {
		return nodeIndex;
	}
}
public class CacheEntry {
	int L1_reads, L1_readMisses, L1_writes, L1_writeMisses, L1_writeBacks, L2_reads, L2_readMisses, L2_writes, L2_writeMisses, L2_writeBacks, MemoryTraffic;
	int evict_in_L1 = 0;
	int globalIndex_Opt = 0;
	public Map<String, String> CacheSS_Map;
	public List<String> CacheS_Data;
	Cache cache_object;
	public CacheEntry(Cache new_Cache, Map<String, String> newCache_SS_Map, List<String> newCache_S_Data) {
		this.cache_object = new_Cache;
		this.CacheSS_Map = newCache_SS_Map;
		this.CacheS_Data = newCache_S_Data;
		L1_reads = 0;
		L1_readMisses =0;
		L1_writes =0;
		L1_writeMisses =0;
		L1_writeBacks = 0;
		L2_reads = 0;
		L2_readMisses =0;
		L2_writes =0;
		L2_writeMisses =0;
		L2_writeBacks = 0;
		insert_dataIn_Cache();
	}

	int get_L1_index(String _L1_index) //Get method for index of the addresses from L1 Cache
	{
		int L1_idx = Integer.parseInt(_L1_index.substring(this.cache_object.L1tag_bit, this.cache_object.L1tag_bit + this.cache_object.L1index_bit), 2);
		return L1_idx;
	}

	int get_L2_index(String _L2_index) //Get method for index of the addresses from L2 cache
	{
		int L2idx = Integer.parseInt(_L2_index.substring(this.cache_object.L2tag_bit, this.cache_object.L2tag_bit + this.cache_object.L2index_bit), 2);
		return L2idx;
	}
	String get_L1_tag(String _L1_tag)
	{
		String L1t = _L1_tag.substring(0, cache_object.L1tag_bit);
		return L1t;
	}
	String get_L2_tag(String str_tag_L2)
	{
		String L2t = str_tag_L2.substring(0, cache_object.L2tag_bit);
		return L2t;
	}

	Map<Integer, List<Node>> hexData = new HashMap<>();
	 public void insert_dataIn_Cache() {
		int j1=0;
			while (j1 < CacheS_Data.size()){
                String data_str = CacheS_Data.get(j1);
                String temp_cData = data_str.split(" ")[1];
                int cache_index = get_L1_index(CacheSS_Map.get(temp_cData));
                if (!hexData.containsKey(cache_index))
                    hexData.put(cache_index, new ArrayList<>());
                hexData.get(cache_index).add(new Node(temp_cData, j1));
                j1++;
            }
        
		int j2=0;
		while(j2< CacheS_Data.size()){
			String Cdata_str = CacheS_Data.get(j2);
			globalIndex_Opt = j2;
			boolean Cdata_read_write = Objects.equals(Cdata_str.split(" ")[0], "r");
			Cdata_str = Cdata_str.split(" ")[1];
			if(Cdata_read_write)
				L1_cache_read(Cdata_str, CacheSS_Map.get(Cdata_str));
			else
				L1_write(Cdata_str, CacheSS_Map.get(Cdata_str));
			j2++;
		}
		if(cache_object.new_cb_L2.isEmpty())
		{
			MemoryTraffic = L1_readMisses + L1_writeMisses + L1_writeBacks;
		}
		else
			MemoryTraffic = L2_readMisses + L2_writeMisses + L2_writeBacks + evict_in_L1;
		CacheOutput();
	}
	int L1_empty_Flag = 0;
	List<Integer> L1_empty_index = new ArrayList<>();
	int rows_index = 0;
	//reading L1 cache
	void L1_cache_read(String L1_data_rd, String L1_bits_rd) {
		List<blockCache> rdL1_list = cache_object.new_cb_L1.get(get_L1_index(L1_bits_rd));
		String rL1_tag = get_L1_tag(L1_bits_rd);
		L1_reads++;
		for(blockCache bc: rdL1_list)
		{
			if(bc.bc_tag.equals(rL1_tag)) {
				L1hit_Read(rL1_tag, rdL1_list, bc);
				return;
			}
		}
		rows_index = get_L1_index(L1_bits_rd);
		L1_readMisses++;
		//if the cache is empty, fill the data and decrease the lru counter's value
		if(rdL1_list.size()< cache_object.L1set_bit)
		{
            rdL1_list.forEach(cb -> cb.set_bc_LRU_Counter(cb.get_bc_LRU_Access_Counter() - 1));
			if(L1_empty_Flag != 0)
			{
				rdL1_list.add(L1_empty_index.get(0),new blockCache(L1_data_rd, rL1_tag, cache_object.L1set_bit -1 , false));
				L1_empty_Flag--;
			}
			else
			{
				rdL1_list.add(new blockCache(L1_data_rd, rL1_tag, cache_object.L1set_bit -1 , false));
			}
			if(!cache_object.new_cb_L2.isEmpty())
			{
				L2_cache_read(L1_data_rd, L1_bits_rd, false, null);
			}
		}
		else //Using the replacement policy
		{
			update_L1_Cache(L1_data_rd, rL1_tag, rdL1_list, true);
		}
	}
	//method to read L1 hits
	void L1hit_Read(String L1_hR_tag, List<blockCache> L1_hR_list, blockCache L1_hRd_c) {
		//lru
		int L1_hRd_val = L1_hRd_c.get_bc_LRU_Access_Counter();
        L1_hR_list.forEach(bc -> {
            if (L1_hRd_val < bc.get_bc_LRU_Access_Counter()) {
                bc.set_bc_LRU_Counter(bc.get_bc_LRU_Access_Counter() - 1);
            } else if (Objects.equals(bc.bc_tag, L1_hR_tag)) {
                bc.set_bc_LRU_Counter(cache_object.L1set_bit - 1);
            }
        });
	}
	//method to write in L1 Cache
	void L1_write(String L1_write, String L1_w_bits) {
		List<blockCache> L1Cache_list = cache_object.new_cb_L1.get(get_L1_index(L1_w_bits));
		String L1_tag = get_L1_tag(L1_w_bits);
		L1_writes++;
		for(blockCache bc: L1Cache_list)
		{
			if(bc.bc_tag.equals(L1_tag)) {
				L1hit_Write(L1_tag, L1Cache_list, bc);
				bc.set_block_cache_dirtyBit(true);
				return;
			}
		}
		rows_index = get_L1_index(L1_w_bits);
		L1_writeMisses++;
		//if the cache is empty, fill the data and decrease the lru counter's value
		if(L1Cache_list.size()< cache_object.L1set_bit)
		{
            L1Cache_list.forEach(cb -> cb.set_bc_LRU_Counter(cb.get_bc_LRU_Access_Counter() - 1));
			if(L1_empty_Flag != 0)
			{
				L1Cache_list.add(L1_empty_index.get(0),new blockCache(L1_write, L1_tag, cache_object.L1set_bit -1 , true));
				L1_empty_Flag--;
			}
			else
			{
				L1Cache_list.add(new blockCache(L1_write, L1_tag, cache_object.L1set_bit -1 , true));
			}
			if(!cache_object.new_cb_L2.isEmpty())
			{
				L2_cache_read(L1_write, L1_w_bits, false, null);
			}
		}
		else //using replacement policy
		{
			update_L1_Cache(L1_write, L1_tag, L1Cache_list, false);
		}
	}
	//method for hit write in the L1 cache
	void L1hit_Write(String L1_tag, List<blockCache> L1_list, blockCache L1_bc) {

		int L1_hW_val = L1_bc.get_bc_LRU_Access_Counter();

        L1_list.forEach(cBlock -> {
            if (cBlock.bc_tag.equals(L1_tag)) {

                cBlock.set_bc_LRU_Counter(cache_object.L1set_bit - 1);
            } else if (cBlock.get_bc_LRU_Access_Counter() > L1_hW_val) {
                cBlock.set_bc_LRU_Counter(cBlock.get_bc_LRU_Access_Counter() - 1);

            }
        });
	}
	//method for reading in L2 cache
	void L2_cache_read(String rd_L1_data, String rd_L1_bits, boolean rd_L1_Evict, blockCache rd_L1_block_evicted) {

		List<blockCache> rd_L1_list = cache_object.new_cb_L2.get(get_L2_index(rd_L1_bits));
		String rd_L1_tag = get_L2_tag(rd_L1_bits);

		if(rd_L1_Evict)
		{
			write_L2(rd_L1_block_evicted.get_bc_data(), CacheSS_Map.get(rd_L1_block_evicted.get_bc_data()));
		}
		L2_reads++;
		for(blockCache bc: rd_L1_list)
		{
			if(bc.get_bc_Tag().equals(rd_L1_tag)) {
				L2hit_read(rd_L1_tag, rd_L1_list, bc);
				return;
			}
		}
		L2_readMisses++;
		rows_index = get_L1_index(rd_L1_bits);
		//if the cache is empty, fill the data and decrease the lru counter's value
		if(rd_L1_list.size()< cache_object.L2set_bit)
		{
            rd_L1_list.forEach(bc -> bc.set_bc_LRU_Counter(bc.get_bc_LRU_Access_Counter() - 1));
			rd_L1_list.add(new blockCache(rd_L1_data, rd_L1_tag, cache_object.L2set_bit -1 , false));
		}
		else //using replacement policy
		{
			update_L2_Cache(rd_L1_data, rd_L1_tag, rd_L1_list, true);
		}
	}
	//method for L2 cache hit while read
	void L2hit_read(String L2_hR_tag, List<blockCache> L2_hR_list, blockCache bc_L2_hR) {
		int L2_hR_val = bc_L2_hR.get_bc_LRU_Access_Counter();

        L2_hR_list.forEach(bc -> {
            if (bc.bc_tag.equals(L2_hR_tag)) {
                bc.set_bc_LRU_Counter(cache_object.L2set_bit - 1);
            } else if (bc.get_bc_LRU_Access_Counter() > L2_hR_val) {
                bc.set_bc_LRU_Counter(bc.get_bc_LRU_Access_Counter() - 1);
            }
        });
	}
	//write in L2 Cache
	void write_L2(String L2_w_data, String L2_w_bits) {
		List<blockCache> L2w_list = cache_object.new_cb_L2.get(get_L2_index(L2_w_bits));
		String L2_w_tag = get_L2_tag(L2_w_bits);
		L2_writes+=1;

		for(blockCache bc: L2w_list)
		{
			if(bc.get_bc_Tag().equals(L2_w_tag)) {
				L2hit_Write(L2_w_tag, L2w_list, bc);
				bc.set_block_cache_dirtyBit(true);
				return;
			}
		}
		L2_writeMisses++;
		rows_index = get_L1_index(L2_w_bits);
		//if empty cache, include data also lru counter value to be decreased
		if(L2w_list.size()< cache_object.L2set_bit)
		{
            L2w_list.forEach(bc -> bc.set_bc_LRU_Counter(bc.get_bc_LRU_Access_Counter() - 1));
			L2w_list.add(new blockCache(L2_w_data, L2_w_tag, cache_object.L2set_bit -1 , true));
		}
		else //applying replacement policy
		{
			update_L2_Cache(L2_w_data, L2_w_tag, L2w_list, false);
		}
	}
	//hit writes for L2 cache
	void L2hit_Write(String L2_hW_tag, List<blockCache> L2_hW_list, blockCache bc_hWL2) {
		int L2_hW_val = bc_hWL2.get_bc_LRU_Access_Counter();

        L2_hW_list.forEach(bc -> {
            if (bc.bc_tag.equals(L2_hW_tag)) {

                bc.set_bc_LRU_Counter(cache_object.L2set_bit - 1);
            } else if (bc.get_bc_LRU_Access_Counter() > L2_hW_val) {
                bc.set_bc_LRU_Counter(bc.get_bc_LRU_Access_Counter() - 1);
            }
        });
	}

	//cache replacement policy for L1 cache
	void update_L1_Cache(String updData, String updTag, List<blockCache> cacheUpdList, boolean readUpd) {
		int upCL1_index = 0;
        if (cache_object.replacementPolicy == 0) {//lru
            int i3 = 0;
            while (i3 < cacheUpdList.size()) {
                blockCache cb = cacheUpdList.get(i3);
                if (cb.get_bc_LRU_Access_Counter() == 0) {
                    upCL1_index = i3;
                } else {
                    cb.set_bc_LRU_Counter(cb.get_bc_LRU_Access_Counter() - 1);
                }
                i3++;
            }

        } else if (cache_object.replacementPolicy == 1) {//fifo
            int required_index = 0;
            int first_in_val = Integer.MAX_VALUE;
            // For iteration to find the value first in for FIFO
            for (int i = 0; i < cacheUpdList.size(); i++) {
                if (cacheUpdList.get(i).bc_FIFO_curr_position < first_in_val) {
                    first_in_val = cacheUpdList.get(i).bc_FIFO_curr_position;
                    required_index = i;
                }
            }
            upCL1_index = required_index;
        }

		blockCache evicted_bc = cacheUpdList.remove(upCL1_index);
		if(evicted_bc.isDirtyBit())
		{
			L1_writeBacks++;
		}
		cacheUpdList.add(upCL1_index, new blockCache(updData, updTag, cache_object.L1set_bit -1 , true));
		if(readUpd)
		{
			cacheUpdList.get(upCL1_index).set_block_cache_dirtyBit(false);
		}
		if(!cache_object.new_cb_L2.isEmpty())
		{
			if(evicted_bc.isDirtyBit())
				write_L2(evicted_bc.get_bc_data(), CacheSS_Map.get(evicted_bc.get_bc_data()));

			L2_cache_read(updData, CacheSS_Map.get(updData), false, null);
		}
	}
	//L2 replacement policy
	void update_L2_Cache(String UpCL2_data, String UpCL2_tag, List<blockCache> UpCL2_list, boolean UpCL2_read) {
		int UpCL2_index = 0;
        if (cache_object.replacementPolicy == 0) {
            int i6 = 0;
            while (i6 < UpCL2_list.size()) {
                blockCache cb = UpCL2_list.get(i6);
                if (cb.get_bc_LRU_Access_Counter() == 0) {
                    UpCL2_index = i6;
                } else {
                    cb.set_bc_LRU_Counter(cb.get_bc_LRU_Access_Counter() - 1);
                }
                i6++;
            }
        }
		else if (cache_object.replacementPolicy == 1) {
			int required_index = 0;
			int first_in_val = Integer.MAX_VALUE;
            int i = 0;
            while (i < UpCL2_list.size()) {
                if (UpCL2_list.get(i).bc_FIFO_curr_position < first_in_val) {
                    first_in_val = UpCL2_list.get(i).bc_FIFO_curr_position;
                    required_index = i;
                }
                i++;
            }
            UpCL2_index = required_index;
        }

		blockCache evicted_now = UpCL2_list.remove(UpCL2_index);
		if(evicted_now.isDirtyBit())
		{
			L2_writeBacks++;
		}
		UpCL2_list.add(UpCL2_index, new blockCache(UpCL2_data, UpCL2_tag, cache_object.L2set_bit -1 , true));
		if(UpCL2_read)
		{
			UpCL2_list.get(UpCL2_index).set_block_cache_dirtyBit(false);
		}
		if(cache_object.inclusionProperty == 1)
		{
			L1CacheEvict(evicted_now);
		}
	}
	void L1CacheEvict(blockCache victim) {
		int index_L1_ev = get_L1_index(CacheSS_Map.get(victim.get_bc_data()));
		String eL1_tag = get_L1_tag(CacheSS_Map.get(victim.get_bc_data()));
		List<blockCache> list_bc = cache_object.new_cb_L1.get(index_L1_ev);
		for(blockCache bc: list_bc)
		{
			if(bc.get_bc_Tag().equals(eL1_tag))
			{
				int index = list_bc.indexOf(bc);
                L1_empty_Flag += 1;
				L1_empty_index.add(index);
				blockCache evict_L1_var = list_bc.remove(index);
				if(evict_L1_var.isDirtyBit())
                    evict_in_L1 += 1;
				break;
			}
		}
	}
	void CacheOutput() {
		System.out.println("===== Simulator configuration =====\n" +
				"BLOCKSIZE:             " + cache_object.blockSize + "\n" +
				"L1_SIZE:               " + cache_object.L1size + "\n" +
				"L1_ASSOC:              " + cache_object.L1Assoc + "\n" +
				"L2_SIZE:               " + cache_object.L2size + "\n" +
				"L2_ASSOC:              " + cache_object.L2Assoc + "\n" +
				"REPLACEMENT POLICY:    " + (cache_object.replacementPolicy == 0 ? "LRU" : (cache_object.replacementPolicy == 1 ? "FIFO " : "Incorrect")) + "\n" +
				"INCLUSION PROPERTY:    " + (cache_object.inclusionProperty == 0 ? "non-inclusive" : "inclusive") + "\n" +
				"trace_file:            " + cache_object.trace_File + "\n" +
				"===== L1 contents =====");


		for (int i = 0; i < cache_object.new_cb_L1.size(); i+=1) {
			if(i<10)System.out.print("Set     " + i + ":     ");
			else System.out.print("Set     " + i + ":    ");
			for (blockCache cb : cache_object.new_cb_L1.get(i)) {
				System.out.print(" " + binToHex(cb.get_bc_Tag()) + (cb.isDirtyBit() ? " D " : "   "));
			}
			System.out.println();
		}

		if(!cache_object.new_cb_L2.isEmpty())
		{
			System.out.println("===== L2 contents =====");

			for (int i = 0; i < cache_object.new_cb_L2.size(); i+=1) {
				if(i<10)System.out.print("Set     " + i + ":     ");
				else if(i<100) System.out.print("Set     " + i + ":    ");
				else System.out.print("Set     " + i + ":   ");
				for (blockCache cb : cache_object.new_cb_L2.get(i)) {
					System.out.print(" " + binToHex(cb.get_bc_Tag()) + (cb.isDirtyBit() ? " D  " : "    "));
				}
				System.out.println();
			}

		}
		System.out.println("===== Simulation results (raw) =====\n" +
				"a. number of L1 reads:        " + L1_reads + "\n" +
				"b. number of L1 read misses:  " + L1_readMisses + "\n" +
				"c. number of L1 writes:       " + L1_writes + "\n" +
				"d. number of L1 write misses: " + L1_writeMisses + "\n" +
				"e. L1 miss rate:              " + (L1CacheMissRate() == 0 ? 0 : String.format("%.6f", L1CacheMissRate())) + "\n" +
				"f. number of L1 writebacks:   " + L1_writeBacks + "\n" +
				"g. number of L2 reads:        " + L2_reads + "\n" +
				"h. number of L2 read misses:  " + L2_readMisses + "\n" +
				"i. number of L2 writes:       " + L2_writes + "\n" +
				"j. number of L2 write misses: " + L2_writeMisses + "\n" +
				"k. L2 miss rate:              " + (L2CacheMissRate() == 0 ? 0 : String.format("%.6f", L2CacheMissRate())) + "\n" +
				"l. number of L2 writebacks:   " + L2_writeBacks + "\n" +
				"m. total memory traffic:      " + MemoryTraffic);
	}
	double L1CacheMissRate() {
		double v1 = (double) (L1_readMisses + L1_writeMisses) / (double) (L1_reads + L1_writes);
		return v1;
	}
	double L2CacheMissRate() {
		double v2 = (double) (L2_readMisses) / (double) (L1_readMisses + L1_writeMisses);
		return v2;
	}
	static String binToHex(String binaryString) {
		BigInteger decimalValue = new BigInteger(binaryString, 2);
		return decimalValue.toString(16);
	}
}
class Cache {
	List<List<blockCache>> new_cb_L1, new_cb_L2;
	int L1tag_bit, L1index_bit, offset_bit, L1set_bit, L2tag_bit, L2index_bit, L2set_bit,
			replacementPolicy,inclusionProperty, blockSize, L1size, L1Assoc, L2size, L2Assoc;
	String trace_File;
	int log_2(int j) {
        return (int) ((Math.log(j)) / (Math.log(2)));
	}
	// Defining the Cache Memory Structure
	Cache(int cons_L1index, int cons_L1set, int cons_L2index, int cons_L2set, int cons_blockSize, int cons_Rp, int cons_Ip, int cons_L1size, int cons_L2size, int cons_L1Assoc, int cons_L2Assoc, String cons_trace_File)
	{
		this.inclusionProperty = cons_Ip;
		this.replacementPolicy = cons_Rp;
		this.blockSize = cons_blockSize;
		this.L1set_bit = cons_L1set;
		this.L2set_bit = cons_L2set;
		new_cb_L1 = new ArrayList<>();
		new_cb_L2 = new ArrayList<>();
		List<blockCache> new_bc_temp;
		int j9=0;
		while(j9<cons_L1index){
			new_bc_temp = new ArrayList<>();
			new_cb_L1.add(new_bc_temp);
			j9++;
		}
		int j10 = 0;
		while(j10<cons_L2index){
			new_bc_temp = new ArrayList<>();
			new_cb_L2.add(new_bc_temp);
			j10++;
		}
		offset_bit = log_2(blockSize);
		this.L1index_bit = log_2(cons_L1index);
		this.L2index_bit = log_2(cons_L2index);
		L1tag_bit = 32 - (this.L1index_bit + offset_bit);
		L2tag_bit = 32 - (this.L2index_bit + offset_bit);
		this.L1size = cons_L1size;
		this.L2size = cons_L2size;
		this.L1Assoc = cons_L1Assoc;
		this.L2Assoc = cons_L2Assoc;
		this.trace_File = cons_trace_File;
	}

}
class CachingManager {
	int CM_BlockSize, CM_L1_size, CM_L1_assoc, CM_L2_size, CM_L2_assoc, CM_Replacement_Policy, CM_Inclusion_Property, CM_L1_sets, CM_L2_sets;

	//declaring a list and a map
	final List<String> DataObj;
	final Map<String,String> MapObj;
	final Cache CacheObj;
	String trace_file;

	// Retrieving command line input parameters
	public CachingManager(int new_CM_BlockSize, int new_CM_L1_size, int new_CM_L1_assoc, int new_CM_L2_size, int new_CM_L2_assoc, int new_CM_Replacement_Policy,
						  int new_CM_Inclusion_Property, String new_traceFile) throws IOException {
		this.CM_BlockSize = new_CM_BlockSize;
		this.CM_L1_size = new_CM_L1_size;
		this.CM_L1_assoc = new_CM_L1_assoc;
		this.CM_L2_size = new_CM_L2_size;
		this.CM_L2_assoc = new_CM_L2_assoc;
		this.CM_Replacement_Policy = new_CM_Replacement_Policy;
		this.CM_Inclusion_Property = new_CM_Inclusion_Property;
		this.trace_file = new_traceFile;

		// Evaluating the rows and sets of L1 and L2 caches and organizing the cache architecture structure.
		CM_Calc_sets_fun();

		CacheObj = new Cache(CM_L1_sets, new_CM_L1_assoc, CM_L2_sets, new_CM_L2_assoc, new_CM_BlockSize, new_CM_Replacement_Policy, new_CM_Inclusion_Property, new_CM_L1_size, new_CM_L2_size, new_CM_L1_assoc, new_CM_L2_assoc, new_traceFile);
		// Retrieving data from an ArrayList and a HashMap
		DataObj = new ArrayList<>();
		MapObj = new HashMap<>();

		CM_readFile_fun(); // Streamlining the data after importing
		CM_insert_fun(); // Inserting the data in the cache for future retrieval
	}
	void CM_insert_fun() {
		new CacheEntry(CacheObj, MapObj, DataObj);
	}
	void CM_Calc_sets_fun() {
		// Ensuring that associativity is positive
        CM_L1_assoc = (CM_L1_assoc == 0) ? 1 : CM_L1_assoc;
        CM_L2_assoc = (CM_L2_assoc == 0) ? 1 : CM_L2_assoc;

		// Calculating sets of L1 and L2 Cache
		CM_L1_sets = CM_L1_size / (CM_L1_assoc * CM_BlockSize);
		CM_L2_sets = CM_L2_size / (CM_L2_assoc * CM_BlockSize);
	}

	// Processing the trace file to extract and retrieve the data.
	void CM_Trace_Data_parse_fun(String str_tf) {
		str_tf = str_tf.trim();
		String ptr;

		// Separating the instruction line into action and hex code
		ptr = str_tf = str_tf.split(" ")[1];
		if(MapObj.containsKey(ptr))
			return;

		// Padding with '0' if the instruction hex code is not 8 bits in length.
		String zero_padding = "00000000";
		if(ptr.length() != 8)
		{
			ptr = zero_padding.substring(0 , 8 - str_tf.length()) + ptr;
		}
		MapObj.put(str_tf, Base_conversion.hexToBin(ptr));
	}
	void CM_readFile_fun() throws IOException {
		//getting and understanding trace file and applying exception if file not found
		File importedFile= new File("traces/" + trace_file);
		BufferedReader b_read = new BufferedReader(new FileReader(importedFile));
		String newStr;

		//check whether line containing instruction is valid or not?
			while ((newStr = b_read.readLine()) != null) {
                if (newStr.isEmpty()) // Skip if not receiving instruction
					continue;

                DataObj.add(newStr); //instruction append
                CM_Trace_Data_parse_fun(newStr);
            }

		//exit stream
		b_read.close();
	}

}
class blockCache {
	int bc_Counter_LRU; //Access Counter for LRU policy
	int bc_FIFO_curr_position; //to keep track of position each time
	static int bc_FIFO_AccessCounter = 0; //Access Counter for FIFO policy
	boolean bc_dirtyBit;
	String bc_data, bc_tag;

	public blockCache(String cons_bc_data, String cons_bc_tag, int cons_bc_AccessCounter_LRU, boolean cons_bc_dirtyBit) {
		super();
		this.bc_data = cons_bc_data;
		this.bc_tag = cons_bc_tag;
		bc_Counter_LRU = cons_bc_AccessCounter_LRU;
		this.bc_dirtyBit = cons_bc_dirtyBit;
		this.bc_FIFO_curr_position = bc_FIFO_AccessCounter+1;
	}
	public String get_bc_data() {
		return bc_data;
	}
	public String get_bc_Tag() {
		return bc_tag;
	}
	public int get_bc_LRU_Access_Counter() {
		return bc_Counter_LRU;
	}
	public void set_bc_LRU_Counter(int _bc_LRU_AccessCounter) {
		bc_Counter_LRU = _bc_LRU_AccessCounter;
	}
	public boolean isDirtyBit() {
		return bc_dirtyBit;
	}
	public void set_block_cache_dirtyBit(boolean arg_dirtyBit) {
		this.bc_dirtyBit = arg_dirtyBit;

	}
}
