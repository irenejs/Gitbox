package net.irenejs.gitbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.Assert;
import org.junit.Test;

public class GitboxTest {
	
	public static void deleteDir(Path directory) throws IOException {
		Files.walk(directory, FileVisitOption.FOLLOW_LINKS)
		    .sorted(Comparator.reverseOrder())
		    .map(Path::toFile)
		    .peek(System.out::println)
		    .forEach(File::delete);
	}

	@Test
	public void testCreateNewLocal() throws Exception {
		Path path = Paths.get("/tmp/test");
		if (Files.exists(path)) {
			deleteDir(path);
		}
		
		GitboxStateProvider stateProvider = new GitboxStateProvider();
		ISynchronizationProvider synchProvider = new JGitSyncProvider(path, stateProvider);
		synchProvider.localCreateNew();
		
		Assert.assertTrue(synchProvider.localExists());
	}
}
