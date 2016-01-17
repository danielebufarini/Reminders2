package com.danielebufarini.reminders2.synchronisation.commands;

public interface Command {
	public void execute(); // may throws GoogleIOException (runtime exception)
}
