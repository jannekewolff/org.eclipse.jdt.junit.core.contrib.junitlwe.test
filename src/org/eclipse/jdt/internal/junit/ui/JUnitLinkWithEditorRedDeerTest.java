package org.eclipse.jdt.internal.junit.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.jboss.reddeer.swt.wait.AbstractWait.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.eclipse.jdt.ui.junit.JUnitView;
import org.jboss.reddeer.eclipse.ui.views.contentoutline.OutlineView;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.condition.JobIsRunning;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.toolbar.ViewToolItem;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.reddeer.swt.wait.TimePeriod;
import org.jboss.reddeer.swt.wait.WaitWhile;
import org.jboss.reddeer.workbench.exception.WorkbenchPartNotFound;
import org.jboss.reddeer.workbench.impl.editor.DefaultEditor;
import org.jboss.reddeer.workbench.impl.editor.TextEditor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class JUnitLinkWithEditorRedDeerTest {

	@Rule
	public MethodRule toogleLinkWithEditor = new MethodRule() {

		@Override
		public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
			final LinkWithEditor linkWithEditor = method.getAnnotation(LinkWithEditor.class);
			final JUnitView junitView = new JUnitView();
			junitView.open();
			sleep(TimePeriod.SHORT);
			ViewToolItem viewToolItem = new ViewToolItem("Link with Editor");
			assertNotNull(viewToolItem);
			if (linkWithEditor == null || linkWithEditor.enabled()) {
				viewToolItem.toggle(true);
			} else {
				viewToolItem.toggle(false);
			}
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					base.evaluate();
				}
			};
		}
	};

	@Before
	public void setupJunitView() {
		// close all editors
		try {
			new DefaultEditor().closeAll(false);
		} catch (WorkbenchPartNotFound e) {
			// ignore if it fails here, it just means there's no editor open
		}
		// run the JUnit tests on the project
    	final ProjectExplorer projectExplorer = new ProjectExplorer();
		assertTrue(projectExplorer.containsProject("JUnit-LWE"));
		projectExplorer.getProject("JUnit-LWE").select();
		new ContextMenu("Run As", "4 JUnit Test").select();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// make sure the view gets updated once the job finished
		sleep(TimePeriod.SHORT);
		assertEquals(new JUnitView().getNumberOfFailures(), 4);
		assertEquals(new JUnitView().getNumberOfErrors(), 0);
	}

	/**
	 * 
	 * @param elements
	 * @return the first {@link TreeItem} in the {@link DefaultTree} whose text
	 *         starts with the given {@code text}
	 */
	private TreeItem getTreeItem(final String... elements) {
		final DefaultTree defaultTree = new DefaultTree();
		defaultTree.setFocus();
		for (TreeItem treeItem : defaultTree.getItems()) {
			if (treeItem.getText().startsWith(elements[0])) {
				if (elements.length == 1) {
					return treeItem;
				}
				return getTreeItem(treeItem, Arrays.copyOfRange(elements, 1, elements.length));
			}
		}
		return null;
	}

	/**
	 * @param parent
	 *            the parent TreeItem
	 * @param elements
	 * @return the first {@link TreeItem} in the {@link DefaultTree} whose text
	 *         starts with the given {@code text}
	 */
	private TreeItem getTreeItem(final TreeItem parent, final String... elements) {
		for (TreeItem treeItem : parent.getItems()) {
			if (treeItem.getText().startsWith(elements[0])) {
				if (elements.length == 1) {
					return treeItem;
				}
				return getTreeItem(treeItem, Arrays.copyOfRange(elements, 1, elements.length));
			}
		}
		return null;
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldOpenEditorWhenDoubleClickOnTestElementInJUnitViewWithLinkEnabled() {
		// given
		new JUnitView().open();
		final TreeItem selectedTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		// when
		selectedTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// then
		final TextEditor defaultEditor = new TextEditor();
		assertTrue(defaultEditor.isActive());
		assertEquals("TP1.java", defaultEditor.getTitle());
		assertEquals("testGetStr1", defaultEditor.getSelectedText());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldOpenEditorWhenDoubleClickOnTestElementInJUnitViewWithLinkDisabled() {
		// given
		new JUnitView().open();
		final TreeItem selectedTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		// when
		selectedTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// then
		final TextEditor defaultEditor = new TextEditor();
		assertTrue(defaultEditor.isActive());
		assertEquals("TP1.java", defaultEditor.getTitle());
		assertEquals("testGetStr1", defaultEditor.getSelectedText());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectElementInEditorWhenSelectingAnotherElementInJUnitViewWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another element in the JUnit view
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem otherTestElement = getTreeItem("junit.lwe.TP1", "testSetStr1");
		otherTestElement.select();
		// then the selected element in the default editor should change, too
		final TextEditor defaultEditor = new TextEditor();
		assertTrue(defaultEditor.isActive());
		assertEquals("TP1.java", defaultEditor.getTitle());
		assertEquals("testSetStr1", defaultEditor.getSelectedText());
		// and in the outline view as well
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectElementInEditorWhenSelectingAnotherElementInJUnitViewWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another element in the JUnit view
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem otherTestElement = getTreeItem("junit.lwe.TP1", "testSetStr1");
		otherTestElement.select();
		// then the selected element in the default editor should not have
		// changed
		final TextEditor defaultEditor = new TextEditor();
		assertTrue(defaultEditor.isActive());
		assertEquals("TP1.java", defaultEditor.getTitle());
		assertEquals("testGetStr1", defaultEditor.getSelectedText());
		// and nor in the outline view
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testGetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}


	@Test
	@LinkWithEditor(enabled = true)
	@Ignore("known to fail for now")
	public void shouldSelectElementInJUnitViewWhenSelectingAnotherElementInOutlineViewWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another element in the outline view
		new OutlineView().open();
		final TreeItem firstOutlineElement = getTreeItem("TP1", "testSetStr1");
		firstOutlineElement.select();
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestSelection = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestSelection.isSelected());
    }

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectElementInJUnitViewWhenSelectingAnotherElementInOutlineViewWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another element in the outline view
		new OutlineView().open();
		final TreeItem firstOutlineElement = getTreeItem("TP1", "testSetStr1");
		firstOutlineElement.select();
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertTrue(initialTestElement.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTestElementInJUnitViewWhenSelectingAnotherMethodNameInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another method name in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectText("testSetStr1");
		sleep(TimePeriod.SHORT);
		// then the JUnit view selection should have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestElementItem = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestElementItem.isSelected());
		// and in the outline view as well
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectTestElementInJUnitViewWhenSelectingAnotherMethodNameInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another method name in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectText("testSetStr1");
		sleep(TimePeriod.SHORT);
		// then the JUnit view selection should not have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertTrue(initialTestElement.isSelected());
		// but the outline view, yes
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTestElementInJUnitViewWhenSelectingAnotherMethodBodyElementInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting a line in another method in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectLine(17);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestSelection = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestSelection.isSelected());
		// and in the outline view as well
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectTestElementInJUnitViewWhenSelectingAnotherMethodBodyElementInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting a line in another method in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectLine(17);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should not have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertTrue(initialTestElement.isSelected());
		// but the outline view, yes
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTestClassInJUnitViewWhenSelectingTypeNameInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting the type name in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectLine(6);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedSelection = getTreeItem("junit.lwe.TP1");
		assertTrue(expectedSelection.isSelected());
		// and in the outline view as well
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectTestClassInJUnitViewWhenSelectingTypeNameInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting the type name in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectLine(6);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should not have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertTrue(initialTestElement.isSelected());
		// but in the outline view, yes
		new OutlineView().open();
		final TreeItem expectedOutlineSelection = getTreeItem("TP1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotChangeSelectedElementInJUnitViewWhenSelectingAnImportStatementInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting a line in the import statements in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectLine(2);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should not have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertTrue(initialTestElement.isSelected());
		// and in the outline view has no selection
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotChangeSelectedElementInJUnitViewWhenSelectingAnImportStatementInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting a line in the import statements in the editor
		final TextEditor editor = new TextEditor();
		editor.activate();
		sleep(TimePeriod.SHORT);
		editor.selectLine(2);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should not have changed
		junitView.activate();
		sleep(TimePeriod.SHORT);
		assertTrue(initialTestElement.isSelected());
		// and in the outline view has no selection
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldActivateOtherEditorViewAndFocusWhenSelectingAnotherElementInJUnitViewWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor firstEditor = new TextEditor();
		assertTrue(firstEditor.isActive());
		assertEquals("TP1.java", firstEditor.getTitle());
		assertEquals("testGetStr1", firstEditor.getSelectedText());
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem secondTestElement = getTreeItem("junit.lwe.TP2", "testSetStr2");
		secondTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor secondEditor = new TextEditor();
		assertTrue(secondEditor.isActive());
		assertEquals("TP2.java", secondEditor.getTitle());
		// focus is on the assertion failure in this case
		assertThat(secondEditor.getSelectedText(), containsString("assertEquals(a.getStr(), \"get\");"));
		// when selecting back the first element in the JUnit view
		sleep(TimePeriod.SHORT);
		junitView.activate();
		sleep(TimePeriod.SHORT);
		final TreeItem thirdTestElement = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		thirdTestElement.select();
		sleep(TimePeriod.getCustom(2));
		// then the first editor should be active and the selection should be
		// correct
		final TextEditor activeEditor = new TextEditor();
		assertEquals(activeEditor.getTitle(), "TP1.java");
		assertEquals(activeEditor.getSelectedText(), "testSetStr1");
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotActivateOtherEditorViewAndFocusWhenSelectingAnotherElementInJUnitViewWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor firstEditor = new TextEditor();
		assertTrue(firstEditor.isActive());
		assertEquals("TP1.java", firstEditor.getTitle());
		assertEquals("testGetStr1", firstEditor.getSelectedText());
		junitView.open();
		sleep(TimePeriod.SHORT);
		final TreeItem secondTestElement = getTreeItem("junit.lwe.TP2", "testSetStr2");
		secondTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor secondEditor = new TextEditor();
		assertTrue(secondEditor.isActive());
		assertEquals("TP2.java", secondEditor.getTitle());
		// focus is on the assertion failure in this case
		assertThat(secondEditor.getSelectedText(), containsString("assertEquals(a.getStr(), \"get\");"));
		// when selecting back the first element in the JUnit view
		sleep(TimePeriod.SHORT);
		junitView.activate();
		sleep(TimePeriod.SHORT);
		final TreeItem thirdTestElement = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		thirdTestElement.select();
		sleep(TimePeriod.getCustom(2));
		// then the second editor should be active and the selection should be
		// the same as before
		final TextEditor activeEditor = new TextEditor();
		assertEquals(activeEditor.getTitle(), "TP2.java");
		assertThat(activeEditor.getSelectedText(), containsString("assertEquals(a.getStr(), \"get\");"));
	}

}

