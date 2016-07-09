/*
    BeepBeep, an event stream processor
    Copyright (C) 2008-2015 Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uqac.lif.cep.json;

import ca.uqac.lif.cep.Function;
import ca.uqac.lif.cep.FunctionProcessor;
import ca.uqac.lif.cep.UnaryFunction;
import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonParser;
import ca.uqac.lif.json.JsonParser.JsonParseException;

/**
 * Processor that turns input strings into output JSON documents
 */
public class JsonFeeder extends FunctionProcessor
{
	/**
	 * The function associated to this feeder. All instances
	 * of feeder share the same function.
	 */
	private static final transient Function s_function = new JsonParsingFunction();
	
	public JsonFeeder()
	{
		super(s_function);
	}
	
	/**
	 * Function that converts a string into a JSON element
	 */
	public static class JsonParsingFunction extends UnaryFunction<String,JsonElement> 
	{
		/**
		 * The parser used to parse the elements. All instances of the
		 * function share the same parser.
		 */
		protected static final JsonParser s_parser = new JsonParser();
		
		public JsonParsingFunction()
		{
			super();
		}
		
		@Override
		public /*@Nullable*/ JsonElement evaluate(String x)
		{
			try 
			{
				return s_parser.parse(x);
			} 
			catch (JsonParseException e) 
			{
				// Do nothing
			}
			return null;
		}
	}
}
