/*
 * Intake, a command processing library
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) Intake team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package app.ashcon.intake.parametric;

import app.ashcon.intake.CommandCallable;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.InvalidUsageException;
import app.ashcon.intake.InvocationCommandException;
import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.ArgumentParseException;
import app.ashcon.intake.argument.Arguments;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.argument.CommandContext;
import app.ashcon.intake.argument.MissingArgumentException;
import app.ashcon.intake.argument.Namespace;
import app.ashcon.intake.argument.UnusedArgumentException;
import app.ashcon.intake.parametric.handler.ExceptionConverter;
import app.ashcon.intake.parametric.handler.InvokeHandler;
import app.ashcon.intake.parametric.handler.InvokeListener;
import app.ashcon.intake.util.auth.AuthorizationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base class for commands that use {@link ArgumentParser}.
 */
public abstract class AbstractParametricCallable implements CommandCallable {

    private final ParametricBuilder builder;
    private final ArgumentParser parser;

    private List<? extends Annotation> commandAnnotations = Collections.emptyList();
    private boolean ignoreUnusedFlags = false;
    private Set<Character> unusedFlags = Collections.emptySet();

    /**
     * Create a new instance.
     *
     * @param builder An instance of the parametric builder
     * @param parser The argument parser
     */
    protected AbstractParametricCallable(ParametricBuilder builder, ArgumentParser parser) {
        checkNotNull(builder, "builder");
        checkNotNull(parser, "parser");

        this.builder = builder;
        this.parser = parser;
    }

    /**
     * Get the parametric builder.
     *
     * @return The parametric builder
     */
    protected ParametricBuilder getBuilder() {
        return builder;
    }

    /**
     * Get the argument parser.
     *
     * @return The argument parser
     */
    protected ArgumentParser getParser() {
        return parser;
    }

    /**
     * Get the annotations on the command.
     *
     * @return The annotations on the command
     */
    protected List<? extends Annotation> getCommandAnnotations() {
        return commandAnnotations;
    }

    /**
     * Set the annotations on the command.
     *
     * @param commandAnnotations The annotations on the command
     */
    protected void setCommandAnnotations(List<? extends Annotation> commandAnnotations) {
        this.commandAnnotations = ImmutableList.copyOf(commandAnnotations);
    }

    /**
     * Get whether flags that are not used by any parameter should be
     * ignored so that an {@link UnusedArgumentException} is not
     * thrown.
     *
     * @return Whether unused flags should be ignored
     */
    protected boolean isIgnoreUnusedFlags() {
        return ignoreUnusedFlags;
    }

    /**
     * Set whether flags that are not used by any parameter should be
     * ignored so that an {@link UnusedArgumentException} is not
     * thrown.
     *
     * @param ignoreUnusedFlags Whether unused flags should be ignored
     */
    protected void setIgnoreUnusedFlags(boolean ignoreUnusedFlags) {
        this.ignoreUnusedFlags = ignoreUnusedFlags;
    }

    /**
     * Get a list of flags that should not cause an
     * {@link UnusedArgumentException} to be thrown if they are
     * not consumed by a parameter.
     *
     * @return List of flags that can be unconsumed
     */
    protected Set<Character> getUnusedFlags() {
        return unusedFlags;
    }

    /**
     * Set a list of flags that should not cause an
     * {@link UnusedArgumentException} to be thrown if they are
     * not consumed by a parameter.
     *
     * @param unusedFlags List of flags that can be unconsumed
     */
    protected void setUnusedFlags(Set<Character> unusedFlags) {
        this.unusedFlags = ImmutableSet.copyOf(unusedFlags);
    }

    @Override
    public final boolean call(String stringArguments, Namespace namespace, List<String> parentCommands) throws CommandException, InvocationCommandException, AuthorizationException {
        // Test permission
        if (!testPermission(namespace)) {
            throw new AuthorizationException();
        }

        String calledCommand = !parentCommands.isEmpty() ? parentCommands.get(parentCommands.size() - 1) : "_";
        String[] split = CommandContext.split(calledCommand + " " + stringArguments);


        final CommandContext context = new CommandContext(split, parser.getValueFlags(), false, namespace);
        namespace.put(CommandContext.class, context);

        final CommandArgs commandArgs = Arguments.viewOf(context);
        namespace.put(CommandArgs.class, commandArgs);

        List<InvokeHandler> handlers = new ArrayList<InvokeHandler>();

        // Provide help if -? is specified
        if (context.hasFlag('?')) {
            throw new InvalidUsageException(null, this, parentCommands, true);
        }

        for (InvokeListener listener : builder.getInvokeListeners()) {
            InvokeHandler handler = listener.createInvokeHandler();
            handlers.add(handler);
        }

        try {
            boolean invoke = true;

            // preProcess
            for (InvokeHandler handler : handlers) {
                if (!handler.preProcess(commandAnnotations, parser, commandArgs)) {
                    invoke = false;
                }
            }

            if (!invoke) {
                return true; // Abort early
            }

            final Object[] args = parser.parseArguments(commandArgs, ignoreUnusedFlags, unusedFlags);

            // preInvoke
            for (InvokeHandler handler : handlers) {
                if (!handler.preInvoke(commandAnnotations, parser, args, commandArgs)) {
                    invoke = false;
                }
            }

            if (!invoke) {
                return true; // Abort early
            }

            // invoke
            try {
                builder.getCommandExecutor().submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        AbstractParametricCallable.this.call(args);
                        return null;
                    }
                }, commandArgs).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }

            // postInvoke
            for (InvokeHandler handler : handlers) {
                handler.postInvoke(commandAnnotations, parser, args, commandArgs);
            }

        } catch (MissingArgumentException e) {
            if (e.getParameter() != null) {
                throw new InvalidUsageException("Too few arguments! Expected another '" + e.getParameter().getName() + "'", this, parentCommands, false, e);
            } else {
                throw new InvalidUsageException("Too few arguments!", this, parentCommands, true, e);
            }

        } catch (UnusedArgumentException e) {
            throw new InvalidUsageException("Too many arguments! Did not use '" + e.getUnconsumed() + "'", this, parentCommands, false, e);

        } catch (ArgumentParseException e) {
            throw new InvalidUsageException(e.getMessage(), this, parentCommands, false, e);

        } catch (ArgumentException e) { // Something else wrong with an argument
            throw new InvalidUsageException(e.getMessage(), this, parentCommands, false, e);

        } catch (CommandException e) { // Thrown by commands
            throw e;

        } catch (ProvisionException e) { // Argument binding failed
            throw new InvocationCommandException("Exception binding arguments: " + e.getMessage(), e);

        } catch (InterruptedException e) { // Thrown by execution
            throw new InvocationCommandException("Interruption exception: " + e.getCause().getMessage(), e.getCause());

        } catch (Throwable e) { // Catch all
            for (ExceptionConverter converter : builder.getExceptionConverters()) {
                converter.convert(e);
            }

            throw new InvocationCommandException(e.getMessage(), e);
        }

        return true;
    }

    /**
     * Called with parsed arguments to execute the command.
     *
     * @param args The arguments parsed into the appropriate Java objects
     * @throws Exception on any exception
     */
    protected abstract void call(Object[] args) throws Exception;

    @Override
    public List<String> getSuggestions(String arguments, Namespace locals) throws CommandException {
        return parser.parseSuggestions(arguments, locals);
    }

}