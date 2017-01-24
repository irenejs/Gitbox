package net.irenejs.gitbox;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
	
	private static final int MONITORING_INTERVAL_IN_SEC = 20;

	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("Usage: Main <gitbox local directory> <github repo URL> <Github username> <Github password>");
			System.out.println("e.g. Main /tmp/gitbox https://github.com/gitboxmaster/Gitbox gitboxmaster mypass!123");
			System.exit(0);
		}

		Path localDirAsPath = Paths.get(args[0]);
		boolean existsAndWritable = Files.isWritable(localDirAsPath);
		if (!existsAndWritable)	{
			System.err.println("Specified path does not exist or is not writable - '" + localDirAsPath.toString() + "'");
			System.exit(1);
		}
		
		URL remoteRepoUrl = null;
		try {
			remoteRepoUrl = new URL(args[1]);
		} catch (MalformedURLException e) {
			System.err.println("Invalid remote git server URL");
			System.exit(1);
		}
	
		Gitbox gitbox = new Gitbox(localDirAsPath, remoteRepoUrl, args[2], args[3]);

		gitbox.init();
		
		gitbox.monitor(MONITORING_INTERVAL_IN_SEC);
		
	}

}
