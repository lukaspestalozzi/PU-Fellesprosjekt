package server_client.commands;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import exceptions.ForcedReturnException;
import exceptions.UsernameAlreadyExistsException;
import server_client.Command;
import server_client.RequestHandler;
import server_client.ServerClientHandler;

public class RemoveUserFromGroup extends Command {
	
	@Override
	public String getCommand() {
		return "remove-user-from-group";
	}

	@Override
	public String getDescription() {
		return "Remove a user from a group.";
	}

	@Override
	public String getManual() {
		return getDescription();
	}

	@Override
	public String[] getArguments() {
		return new String[]{
			"username",
			"name"
		};
	}

	@Override
	public String[] getExamples() {
		return new String[0];
	}

	@Override
	public String run(ServerClientHandler handler, List<String> arguments) throws IOException, TimeoutException, InterruptedException, ForcedReturnException {
		
		try {
			if (RequestHandler.removeUserFromGroup(handler.getUser(), arguments.get(0), arguments.get(1)))
				return "User successfully removed from group!";
			else
				return "User couldn't be removed!";
		} catch (Exception e) {
			return "User couldn't be removed!";
		}
	}
}
