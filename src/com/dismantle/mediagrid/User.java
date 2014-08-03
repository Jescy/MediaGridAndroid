package com.dismantle.mediagrid;

import java.io.Serializable;
import java.util.Vector;

class State {
	/**
	 * hasSession: true registered: true upToDate: false
	 */
	boolean hasSession;
	boolean registered;
	boolean upToDate;

	public State() {
		super();
		hasSession = false;
		registered = false;
		upToDate = false;

	}

	public boolean isHasSession() {
		return hasSession;
	}

	public void setHasSession(boolean hasSession) {
		this.hasSession = hasSession;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public boolean isUpToDate() {
		return upToDate;
	}

	public void setUpToDate(boolean upToDate) {
		this.upToDate = upToDate;
	}

}

/**
 * key: user.doc.key, seckey: Whirlpool(ecDH($scope.user.prikey,
 * str2bigInt(user.doc.key, 64))), messages: [], name: name
 * 
 */
class Member implements Serializable {
	private static final long serialVersionUID = 3614346967078129236L;
	String key;
	String seckey;
	Vector<String> messages;
	String name;
	String fingerprint;

	public Member(String key, String seckey, Vector<String> messages,
			String name) {
		super();
		this.key = key;
		this.seckey = seckey;
		this.messages = messages;
		this.name = name;
	}

	public Member() {
		messages = new Vector<String>();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSeckey() {
		return seckey;
	}

	public void setSeckey(String seckey) {
		this.seckey = seckey;
	}

	public Vector<String> getMessages() {
		return messages;
	}

	public void setMessages(Vector<String> messages) {
		this.messages = messages;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

/**
 * _id: "guoliang" _rev: "33-1a9bbf24873a7d4a495bf1c48017cd3f" key:
 * "2lxQZiUZFLUS3giZ3kBKh3QOIG203eYP5LH=LHbyeAK" left: Array[3] rooms: Array[0]
 * type: "USER" __proto__: Object
 */
class UserDoc implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8648491488353532229L;
	String _id;
	String _rev;
	String key;
	Vector<String> left;
	Vector<String> rooms;
	String type;

	public UserDoc() {
		super();
		_id = null;
		_rev = null;
		key = null;
		left = new Vector<String>();
		rooms = new Vector<String>();
		type = "USER";
	}

}

public class User {
	/**
	 * password: "" prikey: "21976266661803895931480994287484"
	 * pubkey:"7TI3gupDGijZ5Nnp_SrHWDSBuPXNayGAbn9xgPCwGMs" room: "General"
	 * state: Object username: "guoliang"
	 */
	String password;
	String prikey;
	String pubkey;
	String room;
	State state;
	String username;

	public User(String password, String prikey, String pubkey, String room,
			State state, String username) {
		super();
		this.password = password;
		this.prikey = prikey;
		this.pubkey = pubkey;
		this.room = room;
		this.state = state;
		this.username = username;
	}

	public User() {
		super();
		password = "";
		prikey = "0";
		pubkey = "0";
		room = "General";
		state = new State();
		username = "";
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPrikey() {
		return prikey;
	}

	public void setPrikey(String prikey) {
		this.prikey = prikey;
	}

	public String getPubkey() {
		return pubkey;
	}

	public void setPubkey(String pubkey) {
		this.pubkey = pubkey;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}

class ChatItem {
	public static final int ITEM_MSG_ALL = 0;
	public static final int ITEM_MSG_USER = 1;
	public static final int ITEM_MSG_ME = 2;

	int itemType;
	String itemMsg;
	
	String chatNick;

	public ChatItem() {
		super();
		itemType = ITEM_MSG_ALL;
		itemMsg = "";
	}
	
}