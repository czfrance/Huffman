import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;

	private TreeMap<Integer, String> paths = new TreeMap<>();
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] encodings = makeCodingsFromTree(root);

		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, out);

		in.reset();
		writeCompressedBits(encodings, in, out);
		out.close();


//		// remove all this code when implementing compress
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
		out.close();
	}
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int magic = in.readBits(BITS_PER_INT);
		if (magic != HUFF_TREE) {
			throw new HuffException("invalid magic number "+magic);
		}

		HuffNode root = readTree(in);
		HuffNode curr = root;

		while (true) {
			int bits = in.readBits(1);

			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}

			else {
				if (bits == 0) {
					curr = curr.myLeft;
				}
				else {
					curr = curr.myRight;
				}

				if (curr.myLeft == null && curr.myRight == null) {
					if (curr.myValue == PSEUDO_EOF) {
						break;
					}
					else {
						out.writeBits(BITS_PER_WORD, curr.myValue);
						curr = root;
					}
				}
			}
		}
		out.close();
	}

	private HuffNode readTree(BitInputStream in) {
		int curr = in.readBits(1);

		if (curr == -1) {
			throw new HuffException("error in writing bits, -1 DNE");
		}

		if (curr == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);

			return new HuffNode(0, 0, left, right);
		}
		else {
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode(value, 0, null, null);
		}
	}

	private void printTree(HuffNode root) {
		if (root == null) {
			return;
		}

		System.out.println(root.myValue);
		printTree(root.myLeft);
		printTree(root.myRight);
	}

	private int[] readForCounts(BitInputStream in) {
		int[] counts = new int[ALPH_SIZE + 1];
		counts[PSEUDO_EOF] = 1;

		while (true) {
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) {
				break;
			}

			counts[val]++;
		}

		return counts;
	}

	private HuffNode makeTreeFromCounts(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();

		for (int i = 0; i < counts.length; i++) {
			pq.add(new HuffNode(i, counts[i], null, null));
		}

		while (pq.size() != 1) {
			HuffNode h1 = pq.remove();
			HuffNode h2 = pq.remove();

			pq.add(new HuffNode(0, h1.myWeight+h2.myWeight, h1, h2));
		}

		return pq.remove();
	}

	private String[] makeCodingsFromTree(HuffNode root) {
		String[] encodings = new String[ALPH_SIZE + 1];
		codingHelper(root, "", encodings);

		return encodings;
	}

	private void codingHelper(HuffNode tree, String path, String[] encodings) {
		if (tree == null) {
			return;
		}

		if (tree.myLeft == null && tree.myRight == null) {
			encodings[tree.myValue] = path;
			return;
		}

		codingHelper(tree.myLeft, path + "0", encodings);
		codingHelper(tree.myRight, path + "1", encodings);
	}

	private void writeTree(HuffNode root, BitOutputStream out) {
		if (root == null) {
			return;
		}

		if (root.myLeft == null && root.myRight == null) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);
		}

		else {
			out.writeBits(1, 0);
			writeTree(root.myLeft, out);
			writeTree(root.myRight, out);
		}
	}

	private void writeCompressedBits(String[] encodings, BitInputStream in, BitOutputStream out) {
		while (true) {
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) {
				break;
			}

			String code = encodings[val];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}

		String code = encodings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code, 2));
	}
}











