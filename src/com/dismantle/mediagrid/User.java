package com.dismantle.mediagrid;

import java.io.Serializable;
import java.util.Vector;

/**
 * Store Member(other user)'s information.
 * @author Jescy
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
 * Store user's document(just the same as that in server)
 */
class UserDoc implements Serializable {

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

/**
 * Store current user's information
 * @author Jescy
 *
 */
public class User {
	String password;
	String prikey;
	String pubkey;
	String room;
	String username;

	public User(String password, String prikey, String pubkey, String room,
			String username) {
		super();
		this.password = password;
		this.prikey = prikey;
		this.pubkey = pubkey;
		this.room = room;
		this.username = username;
	}

	public User() {
		super();
		password = "";
		prikey = "0";
		pubkey = "0";
		room = "General";
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
/**
 * Store chat item information
 */
class ChatItem {
	/**
	 * Message is send to all, such as time message, member arrival or left message
	 */
	public static final int ITEM_MSG_ALL = 0;
	/**
	 * Chat Message from other user
	 */
	public static final int ITEM_MSG_USER = 1;
	/**
	 * Chat Message from me
	 */
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