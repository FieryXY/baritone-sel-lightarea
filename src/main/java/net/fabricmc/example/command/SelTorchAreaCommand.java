package net.fabricmc.example.command;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.selection.ISelection;

import java.util.List;
import java.util.stream.Stream;

public class SelTorchAreaCommand extends Command {

    private IBaritone baritone;

    public SelTorchAreaCommand(IBaritone iBaritone) {
        super(iBaritone, "lightarea");
        this.baritone = iBaritone;
    }

    @Override
    public void execute(String s, IArgConsumer iArgConsumer) {
        iArgConsumer.requireExactly(0);

        ISelection[] selections = baritone.getSelectionManager().getSelections();

        if(selections.length == 0) {
            //throw new CommandInvalidStateException("Must have a selection in order to use this command");
        }
    }

    @Override
    public Stream<String> tabComplete(String s, IArgConsumer iArgConsumer) {
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Light a Given Selection to Prevent Mob Spawning";
    }

    @Override
    public List<String> getLongDesc() {
        //TODO
        return null;
    }
}
