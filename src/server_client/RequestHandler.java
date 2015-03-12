package server_client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import calendar.Calendar;
import calendar.CalendarEntry;
import calendar.Notification;
import dbms.DataBaseManager;
import exceptions.EntryDoesNotExistException;
import exceptions.GroupAlreadyExistsException;
import exceptions.GroupDoesNotExistException;
import exceptions.HasNotTheRightsException;
import exceptions.InvitationDoesNotExistException;
import exceptions.SessionExpiredException;
import exceptions.UserDoesNotExistException;
import exceptions.UserInGroupDoesNotExistsException;
import exceptions.UsernameAlreadyExistsException;
import exceptions.WrongPasswordException;
import user.Group;
import user.User;

public class RequestHandler{

	private static DataBaseManager dbm;
	private static ServerSocket server;
	private static Set<ServerClientHandler> currently_connected;
	
	public static final int PORT = 80;
	public static final long CHECK_FOR_EXPECTED_INPUT_INTERVAL = 500;
	public static final long WAIT_BEFORE_TIMOUT = 60000;
	public static final char STATUS_OK = 'a';
	public static final char STATUS_DISCONNECTED = 's';
	
	public static void main(String[] args) {
		init();
		acceptClients();
		dispose();
	}
	
	private static void init() {
		System.out.println("Staring server...");
		try {
			currently_connected = new HashSet<>();
			dbm = new DataBaseManager();
			server = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
			dispose();
			System.exit(-1);
		}
		System.out.println("Server started");
	}
	
	private static void dispose() {
		System.out.println("Shutting down server...");
		try {
			for (ServerClientHandler handler : currently_connected)
				handler.close();
			server.close();
			dbm.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("Server down");
	}
	
	private static void acceptClients() {
		System.out.println("Accepting new clients");
		try {
			while (!server.isClosed()) {
				Socket new_client = server.accept();
				System.out.println("New client connected, awaiting identification...");
				ServerClientHandler client_handler = new ServerClientHandler(new_client);
				currently_connected.add(client_handler);
				Thread client_handler_thread = new Thread(client_handler);
				client_handler_thread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			dispose();
			System.exit(-1);
		}
	}
	
	public static boolean isLoggedIn(String username) {
		for (ServerClientHandler handler : currently_connected) {
			if (handler.getUser() != null && handler.getUser().getUsername().equals(username)) return true;
		}
		return false;
	}
	
	public static void disconnectUser(ServerClientHandler client) {
		System.out.print("Disconnecting ");
		if (client.getUser() == null ) System.out.println("not identified client");
		else System.out.println(client.getUser().getUsername());
		currently_connected.remove(client);
	}
	
	/* ===============
	 * User functions
	 *================*/ 
	
	public static User logIn(String username, String password) throws UserDoesNotExistException, WrongPasswordException {
		try {
			User existing_user = dbm.getUser(username);
			if (password.equals(existing_user.getPassword())) {
				System.out.println("New user verified as " + existing_user.getUsername());
				return existing_user; //TODO: Make better login
			}
			throw new WrongPasswordException();
		} catch (UserDoesNotExistException | WrongPasswordException e) {
			System.out.println("New user failed login");
			throw e;
		}
	}
	
	private synchronized static void validate(User requestor) throws SessionExpiredException {
		if (requestor == null) {
			System.out.println("Request from unverified user denied");
			throw new SessionExpiredException();
		}
		System.out.println("Request from user " + requestor.getUsername() + " validated");
		//TODO: Make validation function
	}
	
	public synchronized static boolean createUser(User user) throws UsernameAlreadyExistsException {
		return dbm.addUser(user);
	}
	
	public synchronized static boolean editUser(User requestor, User updated_user) throws UserDoesNotExistException, SessionExpiredException, HasNotTheRightsException {
		validate(requestor);
		if (! updated_user.getUsername().equals(requestor.getUsername()))
			throw new HasNotTheRightsException();
		return dbm.editUser(updated_user);
	}

	public synchronized static boolean makeAdmin(User requestor, String username, int entry_id) throws HasNotTheRightsException, EntryDoesNotExistException, UserDoesNotExistException, SessionExpiredException {
		validate(requestor);
		if (!dbm.isAdmin(requestor.getUsername(), entry_id))
			throw new HasNotTheRightsException();
		return dbm.makeAdmin(requestor.getUsername(), username, entry_id);
	}
	
	/* ===============
	 * CalendarEntry functions
	 *================*/
	
	public synchronized static boolean createEntry(User requestor, CalendarEntry entry) throws UserDoesNotExistException, SessionExpiredException {
		validate(requestor);
		return dbm.addEntry(entry, requestor.getUsername());
	}
	
	public synchronized static boolean deleteEntry(User requestor, int entry_id) throws SessionExpiredException, EntryDoesNotExistException, UserDoesNotExistException, HasNotTheRightsException {
		validate(requestor);
		if (!dbm.isAdmin(requestor.getUsername(), entry_id))
			throw new HasNotTheRightsException();
		return dbm.deleteEntry(requestor.getUsername(), entry_id);
	}
	
	public synchronized static boolean editEntry(User requestor, CalendarEntry entry) throws EntryDoesNotExistException, HasNotTheRightsException, UserDoesNotExistException, SessionExpiredException {
		validate(requestor);
		if (!dbm.isAllowedToEdit(requestor.getUsername(), entry.getEntryID()))
			throw new HasNotTheRightsException();
		return dbm.editEntry(entry, requestor.getUsername());
	}
	
	public synchronized static boolean kickUserFromEntry(User requestor, String username, int entry_id) throws EntryDoesNotExistException, UserDoesNotExistException, SessionExpiredException, HasNotTheRightsException, InvitationDoesNotExistException {
		validate(requestor);
		if (!dbm.isAllowedToEdit(requestor.getUsername(), entry_id) || dbm.isAdmin(username, entry_id))
			throw new HasNotTheRightsException();
		return dbm.hideEvent(username, entry_id);
	}
	
	public synchronized static boolean kickGroupFromEntry(User requestor, String groupname, int entry_id) throws GroupDoesNotExistException, UserInGroupDoesNotExistsException, EntryDoesNotExistException, SessionExpiredException, UserDoesNotExistException, HasNotTheRightsException, InvitationDoesNotExistException {
		validate(requestor);
		for (User user : dbm.getGroup(groupname).getUsers())
			if (dbm.isAdmin(user.getUsername(), entry_id))
				throw new HasNotTheRightsException();
		if (!dbm.isAllowedToEdit(requestor.getUsername(), entry_id))
			throw new HasNotTheRightsException();
		return dbm.hideEventGroup(groupname, entry_id);
	}	
	
	public synchronized static boolean inviteUserToEntry(User requestor, String username, int entry_id) throws EntryDoesNotExistException, UserDoesNotExistException, HasNotTheRightsException, SessionExpiredException {
		validate(requestor);
		if (!dbm.isAllowedToEdit(requestor.getUsername(), entry_id))
			throw new HasNotTheRightsException();
		return dbm.inviteUser(requestor.getUsername(), username, entry_id);
	}
	
	public synchronized static boolean inviteGroupToEntry(User requestor, String groupname, int entry_id) throws GroupDoesNotExistException, EntryDoesNotExistException, UserDoesNotExistException, HasNotTheRightsException, SessionExpiredException {
		validate(requestor);
		if (!dbm.isAllowedToEdit(requestor.getUsername(), entry_id))
			throw new HasNotTheRightsException();
		return dbm.inviteGroup(requestor.getUsername(), groupname, entry_id);
	}
	
	/* ===============
	 * Group functions
	 *================*/ 
	
	public synchronized static boolean createGroup(User requestor, Group group) throws UserDoesNotExistException, GroupAlreadyExistsException, UserInGroupDoesNotExistsException, SessionExpiredException {
		validate(requestor);
		return dbm.addGroup(group);
	}
	
	public synchronized static boolean addUserToGroup(User requestor, String username, String groupname) throws UserDoesNotExistException, GroupDoesNotExistException, SessionExpiredException, HasNotTheRightsException {
		validate(requestor);
		if (!dbm.isMemberOf(groupname, requestor.getUsername()))
			throw new HasNotTheRightsException();
		return dbm.addUserToGroup(username, groupname);
	}
	
	public synchronized static boolean removeUserFromGroup(User requestor, String username, String groupname) throws GroupDoesNotExistException, SessionExpiredException, HasNotTheRightsException, UserDoesNotExistException {
		validate(requestor);
		if (!dbm.isMemberOf(groupname, requestor.getUsername()))
			throw new HasNotTheRightsException();
		return dbm.removeUserFromGroup(username, groupname);
	}
	
	
	/* ===============
	 * 'Calendar' functions
	 *================*/ 
	
	public synchronized static Calendar createCalendar(User requestor) throws UserDoesNotExistException, SessionExpiredException {
		validate(requestor);
		return dbm.createCalendar(requestor.getUsername());
	}
	
	public synchronized static boolean invitationAnswer(User requestor, int entry_id, boolean answer) throws EntryDoesNotExistException, UserDoesNotExistException, SessionExpiredException {
		validate(requestor);
		if (answer)
			return dbm.going(requestor.getUsername(), entry_id);
		else
			return dbm.notGoing(requestor.getUsername(), entry_id);
	}
	
	public synchronized static HashSet<Notification> getNotifications(String username) throws UserDoesNotExistException {
		return dbm.getNotificationsForUser(username);
	}
}
