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
package app.ashcon.intake.parametric.provider;

import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.argument.CommandArgs;
import app.ashcon.intake.parametric.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

class ShortProvider implements Provider<Short>, NumberProvider {

  static final ShortProvider INSTANCE = new ShortProvider();

  @Override
  public String getName() {
    return "integer";
  }

  @Override
  public Short get(CommandArgs arguments, List<? extends Annotation> modifiers)
      throws ArgumentException {
    Double v = parseNumericInput(arguments.next());
    if (v != null) {
      short shortValue = v.shortValue();
      validate(shortValue, modifiers);
      return shortValue;
    } else {
      return null;
    }
  }
}
