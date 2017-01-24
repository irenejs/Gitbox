package net.irenejs.gitbox;

import java.net.URL;

public interface ISynchronizationProvider {

	boolean localExists();

	void localCreateNew();

	void localSynch();

	void setRemote(URL remoteUrl, String user, String pwd);

	boolean remoteExists();

	void remoteCreateNew();

	void remoteSynch();

}