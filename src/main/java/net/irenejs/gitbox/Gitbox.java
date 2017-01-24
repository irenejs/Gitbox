package net.irenejs.gitbox;

import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.irenejs.gitbox.IStateProvider.State;


public class Gitbox {
	
	public static final String REPO_NAME = "gitbox";
	
	private final Path localDirPath;
	private final URL remoteURL;
	private final String user;
	private final String pwd;
	
	private transient Timer timer;
	private ISynchronizationProvider localCopy;
	
	private IStateProvider stateProvider;
	
	public Gitbox(Path localDirPath, URL remoteURL, String user, String pwd) {
		this.localDirPath = localDirPath;
		this.remoteURL = remoteURL;
		this.user = user;
		this.pwd = pwd;
	}
	
	public void init() {
		
		stateProvider = new GitboxStateProvider();
		stateProvider.update(State.INITIALIZING);
		
		localCopy = new JGitSyncProvider(localDirPath, stateProvider);
		if (!localCopy.localExists()) {
			localCopy.localCreateNew();
		}
		
		localCopy.setRemote(remoteURL, user, pwd);
		if (!localCopy.remoteExists()) {
			localCopy.remoteCreateNew();
		}
		stateProvider.updateIfNoError(State.READY);
		
	}
	
	public void monitor(int monitoringIntervalInSec) {
		if (timer!=null) {
			timer.cancel();
		}
		timer = new Timer("Gitbox timer", false);
		
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				
				stateProvider.update(State.SYNCHRONIZING, "synchStart", new Date().toString(), "synchEnd", null);
				
				localCopy.localSynch();
				localCopy.remoteSynch();
				
				stateProvider.updateIfNoError(State.READY, "synchEnd", new Date().toString());
			}
		}, monitoringIntervalInSec*1000, monitoringIntervalInSec*1000);
	}
	
	

}
