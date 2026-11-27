package gfc.tools;

public class ReplaceStrings {

	static final String HELP = 
		"Usage: replacestrings <token> <sourcefile> <targetfile>\n" +
		"Purpose: replace all occurences of <token> string in <targetfile> with contents of <sourcefile>.";
	
	public static void main(String[] args) {
		if (args.length != 3) throw new IllegalArgumentException(HELP);

		int r = replaceTokens( 
				args[0], 
				args[1],
				args[2] );
		
		if (r > 0) System.exit(0);
		else System.exit(-1);
	}
	
	
	static int replaceTokens(String token, String source_filename, String target_filename) {
		StringBuffer target = FileToolkit.readFromFile(target_filename);
		StringBuffer source = FileToolkit.readFromFile(source_filename);

		System.out.print(
				"replacing all occurences of '" + token
				+ "' in file '" + target_filename
				+ "' with contents of file '" + source_filename + "'" );

		//search on after the inserted text -- if source contains the token, this would loop forever otherwise
		int count = 0;
		int i = target.indexOf(token);
		while (i >= 0) {
			target.delete(i, i + token.length());
			target.insert(i, source);
			count++;
			i = target.indexOf(token, i + source.length());
		}

		FileToolkit.writeToFile(target, target_filename);

		System.out.print("*** done. " + count + " replaced.");

		return count;
	}
}
