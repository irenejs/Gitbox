package net.irenejs.gitbox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import net.irenejs.gitbox.IStateProvider.State;

public class JGitSyncProvider implements ISynchronizationProvider {
	
	private static final String REMOTE_NAME = "origin";
	private static final String BRANCH_NAME = "master";

	private static Logger LOG = Logger.getLogger(JGitSyncProvider.class);

	private Path localDirPath;
	private IStateProvider stateProvider;

	private transient Git repository;

	private URL remoteUrl;
	private UsernamePasswordCredentialsProvider credentialProvider;

	public JGitSyncProvider(Path localDirPath, IStateProvider stateProvider) {
		this.localDirPath = localDirPath;
		this.stateProvider = stateProvider;
	}
	
	private static StringBuffer gitIgnoreFileContent() {
		StringBuffer result = new StringBuffer();
		result.append(".DS_Store\n");
		return result;
	}
	
	private static Repository getRepo(String localDirPath) throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.setGitDir(new File(localDirPath + File.separator + ".git"));
		builder.readEnvironment();
		builder.findGitDir();
		return builder.build();
	}

	@Override
	public boolean localExists() {
		try {
			Repository repository = getRepo(localDirPath.toString());
			LOG.debug("Checking if local copy exists (" + repository.getDirectory() + "):\t");

			boolean result = BRANCH_NAME.equals(repository.getBranch());
			if (result) {
				this.repository = new Git(repository);
				LOG.debug("Local copy exists");
			} else {
				LOG.debug("Local copy doesn't exists");
			}
			return result;
		} catch (Exception e) {
			stateProvider.update(State.ERROR, "errorSource","local","errorDetails","Error while refreshing local folder - .git is corrupted");
			return false;
		}
	}

	@Override
	public void localCreateNew() {
		LOG.debug("Creating new local copy configuration..."); 
		
		try {
			Git git = Git.init().setDirectory(localDirPath.toFile()).call();
	        Files.write(Paths.get(localDirPath.toString() + File.separator + ".gitignore"), gitIgnoreFileContent().toString().getBytes());
			git.add().addFilepattern(".gitignore").call();
			git.commit().setAll(true).setMessage("Git ignore").call();
			this.repository = git;
			LOG.debug("Preparing directory for local copy: OK");
		} catch (Exception e) {
			LOG.debug("Preparing directory for local copy: Error");
			stateProvider.update(State.ERROR);
		}
	}

	@Override
	public void localSynch() {
		LOG.debug("Starting local synch..."); 
		
		try {
			Status status = repository.status().call();

			if (!status.isClean()) {
				boolean addIsRequired = status.getUntracked() != null || status.getUntrackedFolders() != null;
				if (addIsRequired) {
					LOG.debug("New files or removals detected");
					repository.add().addFilepattern(".").call();
					repository.add().setUpdate(true).addFilepattern(".").call();
				}

				if (status.hasUncommittedChanges() || addIsRequired) {
					if (!addIsRequired) {
						LOG.debug("Updates to existing files detected");
					}
					repository.commit().setAll(true).setMessage(new Date().toString()).call();
				}
			} else {
				LOG.debug("No local changes.");
			}
			LOG.debug("Local synch - OK"); 
		} catch (Exception e) {
			stateProvider.update(State.ERROR);
		}
	}
	
	@Override
	public void setRemote(URL remoteUrl, String user, String pwd) {
		this.remoteUrl = remoteUrl;
		this.credentialProvider = new UsernamePasswordCredentialsProvider(user, pwd);
	}

	@Override
	public boolean remoteExists() {
		LOG.debug("Checking if remote copy exists ("+remoteUrl.toString()+"):");
		
		try {
			 LsRemoteCommand lsCmd = new LsRemoteCommand(null);
			 lsCmd.setRemote(remoteUrl.toString()+".git");
			Collection<?> refs= lsCmd.call();
			boolean result = !refs.isEmpty();
			LOG.debug("Remote copy " + (result ? "" : "doesn't ") + "exists");
			return result;
		} catch (GitAPIException e) {
			stateProvider.update(State.ERROR);
		}
		return false;
	}

	@Override
	public void remoteCreateNew() {
		LOG.debug("Creating new remote copy configuration...");
		
		try {
			RemoteAddCommand remoteAdd = repository.remoteAdd();
			remoteAdd.setName(REMOTE_NAME);
			remoteAdd.setUri(new URIish(remoteUrl));
			remoteAdd.call();
			
			PushCommand pushCommand = repository.push();
			pushCommand.setRemote(REMOTE_NAME);
			pushCommand.setCredentialsProvider(credentialProvider);
			pushCommand.setRefSpecs(new RefSpec("master:master"));
			pushCommand.call();
			
			LOG.debug("Creating new remote copy: OK");
		} catch (Exception e) {
			LOG.debug("Creating new remote copy: ERROR");
			stateProvider.update(State.ERROR);
		}
	}

	@Override
	public void remoteSynch() {
		LOG.debug("Starting remote synch..."); 
		
		try {
			PullResult resultPull = repository.pull()
				.setCredentialsProvider(credentialProvider)
				.setRemote(REMOTE_NAME)
				.setRemoteBranchName(BRANCH_NAME)
				.call();
			
			boolean successful = resultPull.isSuccessful();
			if (!successful) {
				LOG.debug("Remote synch error - not successful pull"); 
				stateProvider.update(State.ERROR);
				return;
			} else {	
				int updatedFiles = resultPull.getFetchResult().getTrackingRefUpdates().size();
				if (updatedFiles > 0) {
					LOG.debug("Remote synch - updated=" + updatedFiles); 
				} else {	
					LOG.debug("No remote changes.");
				}
			}
			
			PushCommand pushCommand = repository.push();
			pushCommand.setRemote(REMOTE_NAME);
			pushCommand.setCredentialsProvider(credentialProvider);
			pushCommand.setRefSpecs(new RefSpec(BRANCH_NAME+":"+BRANCH_NAME));
			pushCommand.call();
			
			LOG.debug("Remote synch: OK");
		} catch (Exception e) {
			LOG.debug("Remote synch: ERROR");
			stateProvider.update(State.ERROR);
		}
	}

}
