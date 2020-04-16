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
package app.ashcon.intake;

import java.util.List;

/** Defines a parameter for a command. */
public interface Parameter {

  /**
   * The name of the parameter.
   *
   * @return The name of the parameter
   */
  String getName();

  /**
   * The type of parameter.
   *
   * @return The type of parameter
   */
  OptionType getOptionType();

  /**
   * Whether the parameter is wrapped in an {@link java.util.Optional}.
   *
   * @return Whether the parameter is optional.
   */
  boolean isOptional();

  /**
   * The default value as a list of tokenized strings (but one single value).
   *
   * <p>If there is no default value, the returned list will be empty.
   *
   * @return The default value
   */
  List<String> getDefaultValue();
}
