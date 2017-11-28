/*
    BeepBeep, an event stream processor
    Copyright (C) 2008-2017 Sylvain Hallé

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
package ca.uqac.lif.cep.functions;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ca.uqac.lif.cep.Connector.Variant;
import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Context;
import ca.uqac.lif.cep.ProvenanceTest.DummyTracker;
import ca.uqac.lif.cep.Pushable;
import ca.uqac.lif.cep.Pushable.PushableException;
import ca.uqac.lif.cep.numbers.Addition;
import ca.uqac.lif.cep.numbers.Multiplication;
import ca.uqac.lif.cep.tmf.BlackHole;
import ca.uqac.lif.cep.tmf.Passthrough;

/**
 * Unit tests for functions
 */
public class FunctionsTest
{
	@Test
	public void testNegation() throws FunctionException
	{
		assertEquals(false, evaluate(Negation.instance, true));
		assertEquals(true, evaluate(Negation.instance, false));
	}
	
	@Test
	public void testOr() throws FunctionException
	{
		assertEquals(true, evaluate(Or.instance, true, false));
		assertEquals(false, evaluate(Or.instance, false, false));
	}
	
	@Test
	public void testException() throws FunctionException
	{
		boolean got_exception = false;
		try
		{
			evaluate(new ExceptionFunction(), 3);
		}
		catch (FunctionException e)
		{
			got_exception = true;
			assertNotNull(e.getMessage());
		}
		assertTrue(got_exception);
	}
	
	@Test
	public void testId() throws FunctionException
	{
		IdentityFunction f = new IdentityFunction(1);
		assertEquals(false, evaluate(f, false));
		assertEquals(0, evaluate(f, 0));
		assertEquals(1, f.getInputArity());
		assertEquals(1, f.getOutputArity());
		IdentityFunction f2 = f.duplicate();
		assertEquals(6, evaluate(f2, 6));
		assertEquals(1, f2.getInputArity());
		assertEquals(1, f2.getOutputArity());
		assertEquals(Variant.class, f.getOutputTypeFor(0));
		Set<Class<?>> types = new HashSet<Class<?>>();
		f.getInputTypesFor(types, 0);
		assertEquals(1, types.size());
		assertTrue(types.contains(Variant.class));
	}
	
	@Test
	public void testAnd() throws FunctionException
	{
		assertEquals(false, evaluate(And.instance, true, false));
		assertEquals(true, evaluate(And.instance, true, true));
	}
	
	@Test
	public void testPassthroughFunction() throws FunctionException
	{
		IdentityFunction id = new IdentityFunction(1);
		TestPassthroughFunction ptf = new TestPassthroughFunction();
		assertTrue(ptf.getFunction() instanceof IdentityFunction);
		assertEquals(id.getInputArity(), ptf.getInputArity());
		assertEquals(id.getOutputArity(), ptf.getOutputArity());
		assertEquals(id.getOutputTypeFor(0), ptf.getOutputTypeFor(0));
		Set<Class<?>> types1 = new HashSet<Class<?>>();
		ptf.getInputTypesFor(types1, 0);
		assertTrue(types1.size() == 1 && types1.contains(Variant.class));
		assertEquals(6, evaluate(ptf, 6));
		assertEquals(6, evaluate(ptf, new Context(), 6));
	}
	
	@Test
	public void testContext1() throws FunctionException
	{
		ContextPlaceholder cph = new ContextPlaceholder("a");
		Context c = new Context();
		assertEquals(null, evaluate(cph, c, true, true));
		assertEquals(null, evaluate(cph, true, true));
		c.put("a", 6);
		assertEquals(6, evaluate(cph, c, true, true));
		assertEquals("$a", cph.toString());
		cph.reset(); // Should do nothing
		assertEquals("a", cph.getName());
		Set<Class<?>> types = new HashSet<Class<?>>();
		cph.getInputTypesFor(types, 0);
		assertTrue(types.contains(Variant.class));
		assertEquals(Variant.class, cph.getOutputTypeFor(0));
		assertTrue(cph.equals(cph.duplicate()));
		assertFalse(cph.equals(new ContextPlaceholder("b")));
		assertFalse(cph.equals("b"));
		assertFalse(cph.equals(null));
		assertEquals(0, cph.getInputArity());
		assertEquals(1, cph.getOutputArity());
	}
	
	@Test
	public void testContext2() throws FunctionException
	{
		Context c = new Context();
		c.put("a", 6);
		FunctionTree f = new FunctionTree(Addition.instance, new Constant(3), new ContextPlaceholder("a"));
		assertEquals(9f, evaluate(f, c, 4));
	}
	
	@Test
	public void testArgumentPlaceholder1() throws FunctionException
	{
		ArgumentPlaceholder aph = new ArgumentPlaceholder(0);
		assertEquals("foo", evaluate(aph, "foo", "bar", "baz"));
		ArgumentPlaceholder aph2 = new ArgumentPlaceholder(0);
		assertTrue(aph.equals(aph2));
		assertFalse(aph.equals(new ArgumentPlaceholder(1)));
		assertFalse(aph.equals(1));
		assertFalse(aph.equals(null));
		assertEquals(0, aph.getIndex());
		Set<Class<?>> types = new HashSet<Class<?>>();
		aph.getInputTypesFor(types, 0);
		assertTrue(types.contains(Variant.class));
		assertEquals(Variant.class, aph.getOutputTypeFor(0));
		assertEquals("$0", aph.toString());
		aph.reset(); // Should do nothing
	}
	
	@Test
	public void testFunctionTree1() throws FunctionException
	{
		FunctionTree ft = new FunctionTree(Addition.instance, new Constant(1), new ArgumentPlaceholder(0));
		assertEquals(6f, evaluate(ft, 5));
		FunctionTree ft2 = ft.duplicate();
		assertFalse(ft == ft2);
		assertEquals(6f, evaluate(ft2, 5));
		String msg = ft.toString();
		assertNotNull(msg);
		assertTrue(msg.length() > 0);
		Set<Class<?>> types = new HashSet<Class<?>>();
		ft2.getInputTypesFor(types, 0);
		assertTrue(types.contains(Number.class));
		assertEquals(Number.class, ft2.getOutputTypeFor(0));
	}
	
	@Test
	public void testFunctionTree3() throws FunctionException
	{
		FunctionTree ft = new FunctionTree(IfThenElse.instance, new ArgumentPlaceholder(0), new ArgumentPlaceholder(1), new ArgumentPlaceholder(2));
		assertEquals(6, evaluate(ft, false, 5, 6));
		String msg = ft.toString();
		assertNotNull(msg);
		assertTrue(msg.length() > 0);
		Set<Class<?>> types = new HashSet<Class<?>>();
		ft.getInputTypesFor(types, 0);
		assertTrue(types.contains(Boolean.class));
		types.clear();
		ft.getInputTypesFor(types, 1);
		assertTrue(types.contains(Variant.class));
		assertEquals(Variant.class, ft.getOutputTypeFor(0));
	}
	
	@Test
	public void testFunctionTree2() throws FunctionException
	{
		FunctionTree ft = new FunctionTree(Addition.instance, new Constant(1), new ArgumentPlaceholder(0));
		assertEquals(6f, evaluate(ft, 5));
	}
	
	@Test
	public void testIfThenElse1() throws FunctionException
	{
		assertEquals(3, IfThenElse.instance.getInputArity());
		assertEquals(1, IfThenElse.instance.getOutputArity());
		assertEquals(5, evaluate(IfThenElse.instance, true, 5, 6));
		assertEquals(6, evaluate(IfThenElse.instance, false, 5, 6));
	}
	
	@Test(expected=InvalidArgumentException.class)
	public void testIfThenElse2() throws FunctionException
	{
		evaluate(IfThenElse.instance, "foo", 5, 6);
	}
	
	@Test
	public void testConstant() throws FunctionException
	{
		Constant ct = new Constant(6);
		assertEquals(6, ct.getValue());
		assertEquals(1, ct.getOutputArity());
		assertEquals(0, ct.getInputArity());
		Constant ct2 = ct.duplicate();
		assertFalse(ct == ct2);
		assertEquals(6, ct2.getValue());
		assertEquals(6, evaluate(ct2));
	}
	
	@Test
	public void testConstantNull() throws FunctionException
	{
		Constant ct = new Constant(null);
		assertEquals(null, ct.getValue());
		assertEquals(1, ct.getOutputArity());
		assertEquals(0, ct.getInputArity());
		Constant ct2 = ct.duplicate();
		assertFalse(ct == ct2);
		assertEquals(null, ct2.getValue());
		assertEquals(null, evaluate(ct2));
		assertEquals("null", ct2.toString());
	}
	
	@Test
	public void testProvenance() throws FunctionException
	{
		IdentityFunction id = new IdentityFunction(1);
		FunctionProcessor fp = new FunctionProcessor(id);
		DummyTracker tracker = new DummyTracker();
		fp.setEventTracker(tracker);
		Connector.connect(fp, new BlackHole());
		Pushable p = fp.getPushableInput();
		p.push(6);
		tracker.containsInputAssociation(fp.getId(), 0, 0, 0, 0);
		p.push(7);
		tracker.containsInputAssociation(fp.getId(), 1, 1, 1, 1);
	}
	
	@Test(expected=PushableException.class)
	public void testFunctionProcessorException() throws FunctionException
	{
		ExceptionFunction ef = new ExceptionFunction();
		FunctionProcessor fp = new FunctionProcessor(ef);
		assertEquals(ef, fp.getFunction());
		Connector.connect(fp, new BlackHole());
		Pushable p = fp.getPushableInput();
		p.push(6);
	}
	
	@Test
	public void testEquals() throws FunctionException
	{
		assertEquals(false, (Boolean) evaluate(Equals.instance, 0, null));
		assertEquals(true, (Boolean) evaluate(Equals.instance, 0, 0));
		assertEquals(false, (Boolean) evaluate(Equals.instance, 0, 1));
		assertEquals(false, (Boolean) evaluate(Equals.instance, "0", 0));
		assertEquals(true, (Boolean) evaluate(Equals.instance, "0", "0"));
		assertEquals(false, (Boolean) evaluate(Equals.instance, "0", "1"));
		Set<Integer> s1 = new HashSet<Integer>();
		s1.add(0);
		s1.add(1);
		Set<Integer> s2 = new HashSet<Integer>();
		s2.add(0);
		s2.add(1);
		assertEquals(true, (Boolean) evaluate(Equals.instance, s1, s2));
		assertEquals(false, (Boolean) evaluate(Equals.instance, s1, 0));
		s1.add(2);
		assertEquals(false, (Boolean) evaluate(Equals.instance, s1, s2));
	}
	
	@Test
	public void testEvaluateFast() throws FunctionException
	{
		assertEquals(evaluateFast(And.instance, true, false), evaluate(And.instance, true, false));
	}
	
	@Test
	public void testCumulative1() 
	{
		CumulativeProcessor sum = new CumulativeProcessor(new CumulativeFunction<Number>(Multiplication.instance));
		DummyTracker tracker = new DummyTracker();
		sum.setEventTracker(tracker);
		Connector.connect(sum, new BlackHole());
		Pushable p = sum.getPushableInput();
		int stream_index = 0;
		p.push(2);
		assertTrue(tracker.containsInputAssociation(sum.getId(), stream_index, 0, stream_index, 0));
		p.push(3);
		assertTrue(tracker.containsInputAssociation(sum.getId(), stream_index, 1, stream_index, 1));
		assertTrue(tracker.containsOutputAssociation(sum.getId(), stream_index, 0, stream_index, 1));
	}
	
	@Test
	public void testEmlBoolean1() throws FunctionException
	{
		EmlBoolean eb = new EmlBoolean(true);
		assertEquals(true, evaluate(eb));
		assertEquals(true, EmlBoolean.parseBoolValue(1));
		assertEquals(false, EmlBoolean.parseBoolValue(0));
		assertEquals(true, EmlBoolean.parseBoolValue(true));
		assertEquals(false, EmlBoolean.parseBoolValue(false));
		assertEquals(true, EmlBoolean.parseBoolValue("T"));
		assertEquals(false, EmlBoolean.parseBoolValue("F"));
		assertEquals(true, EmlBoolean.parseBoolValue("true"));
		assertEquals(false, EmlBoolean.parseBoolValue("false"));
		assertEquals(true, EmlBoolean.parseBoolValue("1"));
		assertEquals(false, EmlBoolean.parseBoolValue("0"));
	}
	
	@Test
	public void testContextAssignment1() throws FunctionException
	{
		ContextAssignment ca = new ContextAssignment("a", new Constant(6));
		Object[] ins = new Object[]{};
		Object[] out = new Object[1];
		Context c = new Context();
		ca.assign(ins, out, c);
		assertEquals(6, c.get("a"));
	}
	
	@Test
	public void testContextAssignment2() throws FunctionException
	{
		ContextAssignment ca = new ContextAssignment("a", new Constant(6));
		Object[] ins = new Object[]{};
		Object[] out = new Object[1];
		Passthrough pt = new Passthrough();
		ca.assign(ins, out, pt);
		assertEquals(6, pt.getContext("a"));
	}
	
	public static Object evaluate(Function f, Context c, Object ... inputs) throws FunctionException
	{
		Object[] ins = inputs;
		Object[] out = new Object[1];
		f.evaluate(ins, out, c);
		return out[0];
	}
	
	public static Object evaluate(Function f, Object ... inputs) throws FunctionException
	{
		Object[] ins = inputs;
		Object[] out = new Object[1];
		f.evaluate(ins, out);
		return out[0];
	}
	
	public static Object evaluateFast(Function f, Object ... inputs) throws FunctionException
	{
		Object[] ins = inputs;
		Object[] out = new Object[1];
		f.evaluateFast(ins, out, null);
		return out[0];
	}
	
	public static class ExceptionFunction extends UnaryFunction<Number,Number>
	{
		public ExceptionFunction()
		{
			super(Number.class, Number.class);
		}

		@Override
		public Number getValue(Number x) throws FunctionException 
		{
			throw new FunctionException("foo");
		}
	}
	
	public static class TestPassthroughFunction extends PassthroughFunction
	{
		@Override
		public Function getFunction()
		{
			return new IdentityFunction(1);
		}
	}
}
