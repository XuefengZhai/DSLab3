package configData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.*;

import org.yaml.snakeyaml.Yaml;

public class YamlParser {
	private class Lock {
		public FileLock lock;
		public FileChannel channel;

		public Lock(FileLock lock, FileChannel channel) {
			this.lock = lock;
			this.channel = channel;
		}
	}

	private Yaml yaml;
	private Map<String, Map<String, Object>> map;
	private long lastModifiedTime;

	public HashMap<String, IpPort> nodeList;
	public List<Map<String, Object>> configList;
	public ArrayList<Rule> sendRules;
	public ArrayList<Rule> receiveRules;
	public ArrayList<Rule> logRules;
	public String clockType;
	public HashMap<String, List<String>> groups;

	public YamlParser(String file) throws IOException {
		// initialize variables
		yaml = new Yaml();
		nodeList = new HashMap<String, IpPort>();
		sendRules = new ArrayList<Rule>();
		receiveRules = new ArrayList<Rule>();
		logRules = new ArrayList<Rule>();
		groups = new HashMap<String, List<String>>();
		ReadFile(file);
	}

	@SuppressWarnings({ "unchecked" })
	public void ReadFile(String file) throws IOException {

		File f = new File(file);
		
		// lock file
		Lock lock = LockFile(f);
		
		// read yaml file
		lastModifiedTime = f.lastModified();
		InputStream input = new FileInputStream(f);
		
		map = (Map<String, Map<String, Object>>) yaml.load(input);
		System.out.println("YamlParser After Load");
		configList = (List<Map<String, Object>>) map.get("configuration");
		
		clockType = (String) map.get("clock").get("type");
		
		// release lock
		if (lock != null) {
			if (lock.lock != null) {
				lock.lock.release();
			}
			if (lock.channel != null) {
				lock.channel.close();
			}
		}

		// clear data structure
		nodeList.clear();
		sendRules.clear();
		receiveRules.clear();

		System.out.println("YamlParser Got Configuration");
		
		// build data structure
		List<Map<String, Object>> list = (List<Map<String, Object>>) map
				.get("configuration");
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> m = list.get(i);
			nodeList.put((String) m.get("name"), new IpPort((String) m.get("ip"),
					(Integer) m.get("port"), i));
		}
		
		list = (List<Map<String, Object>>) map.get("groups");
		if (list != null) {
			for (int i = 0; i < list.size(); ++i) {
				Map<String, Object> m = list.get(i);
				String grpName = (String)(m.get("name"));
				List<String> members = (List<String>)(m.get("members"));
				groups.put(grpName, members);
			}
		}

		list = (List<Map<String, Object>>) map.get("sendRules");
		if (list != null) {
			for (Map<String, Object> m : list) {
				sendRules.add(new Rule(m));
			}
		}

		list = (List<Map<String, Object>>) map.get("receiveRules");
		if (list != null) {
			for (Map<String, Object> m : list) {
				receiveRules.add(new Rule(m));
			}
		}
		
		list = (List<Map<String, Object>>) map.get("logRules");
		if (list != null) {
			for (Map<String, Object> m : list) {
				logRules.add(new Rule(m));
			}
		}
	}

	// check file
	// return true if file has not been modified
	// and false if file has been modified since last access
	public boolean CheckFile(String file) {
		File f = new File(file);
		return lastModifiedTime == f.lastModified();
	}

	@SuppressWarnings("resource")
	public Lock LockFile(File file) throws IOException {
		// lock file
		FileChannel channel;
		channel = new RandomAccessFile(file, "rw").getChannel();
		FileLock lock = channel.lock();

		// check wether file has been locked successfully
		try {
			lock = channel.tryLock();
		} catch (OverlappingFileLockException e) {
			// lock file successfully
			return new Lock(lock, channel);
		}
		// lock fails
		System.out.println("Warning! Can't lock file " + file.getName());
		return null;
	}
	
	public HashMap<String, List<String>> getGroups() {
		return groups;
	}
	
	public boolean isNodeExist(String name) {
		return nodeList.containsKey(name);
	}
	
	public boolean isGrpExist(String grpName) {
		return groups.containsKey(grpName);
	}
}
