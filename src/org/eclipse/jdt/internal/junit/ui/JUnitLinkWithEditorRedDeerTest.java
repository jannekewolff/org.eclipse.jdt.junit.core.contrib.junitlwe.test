package org.eclipse.jdt.internal.junit.ui;

import static org.hamcrest.Matchers.containsString;
import static org.jboss.reddeer.swt.wait.AbstractWait.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Item;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.eclipse.jdt.ui.junit.JUnitView;
import org.jboss.reddeer.eclipse.jdt.ui.packageexplorer.ProjectItem;
import org.jboss.reddeer.eclipse.ui.views.contentoutline.OutlineView;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.reddeer.swt.condition.JobIsRunning;
import org.jboss.reddeer.swt.handler.WorkbenchHandler;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.toolbar.ViewToolItem;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.reddeer.swt.wait.TimePeriod;
import org.jboss.reddeer.swt.wait.WaitWhile;
import org.jboss.reddeer.workbench.exception.WorkbenchPartNotFound;
import org.jboss.reddeer.workbench.impl.editor.TextEditor;
import org.jboss.reddeer.workbench.impl.view.AbstractView;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@SuppressWarnings({ "restriction" })
public class JUnitLinkWithEditorRedDeerTest {

	private static final String SYNCED_IMAGE = "synced.gif";
	private static final String LINK_WITH_EDITOR = "Link with Editor";
	private static final String TEST_PROJECT = "JUnit-LWE";
	private static final String SYNC_BROKEN_IMAGE = "sync_broken.gif";

	@Rule
	public MethodRule toogleLinkWithEditor = new MethodRule() {

		@Override
		public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
			final LinkWithEditor linkWithEditor = method.getAnnotation(LinkWithEditor.class);
			final JUnitView junitView = new JUnitView();
			open(junitView);
			ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
			assertNotNull(viewToolItem);
			// enable/disable as requested
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

	private void runAllTests() {
		// run the JUnit tests on the project
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		assertTrue(projectExplorer.containsProject(TEST_PROJECT));
		projectExplorer.getProject(TEST_PROJECT).select();
		new ContextMenu("Run As", "4 JUnit Test").select();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// make sure the view gets updated once the job finished
		sleep(TimePeriod.SHORT);
		assertEquals(new JUnitView().getNumberOfFailures(), 4);
		assertEquals(new JUnitView().getNumberOfErrors(), 0);
	}

	private void runAllNestedTests() {
		// run the JUnit tests on the project
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.open();
		assertTrue(projectExplorer.containsProject(TEST_PROJECT));
		projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar").open();
		new ContextMenu("Run As", "4 JUnit Test").select();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// make sure the view gets updated once the job finished
		sleep(TimePeriod.SHORT);
		assertEquals(new JUnitView().getNumberOfFailures(), 2);
		assertEquals(new JUnitView().getNumberOfErrors(), 0);
	}

	private void closeAllEditors() {
		// close all editors
		try {
			WorkbenchHandler.getInstance().closeAllEditors();
		} catch (WorkbenchPartNotFound e) {
			// ignore if it fails here, it just means there's no editor open
		}

	}

	@Before
	public void setupJunitView() {
		closeAllEditors();
		runAllTests();
	}

	@After
	public void closeJunitView() {
		new JUnitView().close();
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

	private Image getImage(final Item item) {
		final LinkedBlockingQueue<Image> queue = new LinkedBlockingQueue<Image>(1);
		org.jboss.reddeer.swt.util.Display.asyncExec(new Runnable() {
			@Override
			public void run() {
				queue.add(item.getImage());
			}
		});
		try {
			return queue.poll(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("Failed to retrieve the image");
			return null;
		}
	}

	private void open(final AbstractView view) {
		view.open();
		sleep(TimePeriod.SHORT);
	}

	private void open(final ProjectItem item) {
		item.open();
		sleep(TimePeriod.SHORT);
	}

	private void select(final TreeItem treeItem) {
		treeItem.select();
		// sleep(TimePeriod.SHORT);
	}
	private void activate(final AbstractView view) {
		view.activate();
		sleep(TimePeriod.SHORT);
	}

	private void activate(final TextEditor editor) {
		editor.activate();
		sleep(TimePeriod.SHORT);
	}

	private Matcher<Image> matches(final String iconName) {
		return new BaseMatcher<Image>() {

			@Override
			public boolean matches(Object item) {
				final Image image = (Image) item;
				final LinkedBlockingQueue<ImageData> queue = new LinkedBlockingQueue<ImageData>(1);
				org.jboss.reddeer.swt.util.Display.syncExec(new Runnable() {
					@Override
					public void run() {
						final IPath path = JavaPluginImages.ICONS_PATH.append("elcl16").append(iconName);
						final ImageDescriptor imageDescriptor = JavaPluginImages.createImageDescriptor(JavaPlugin
								.getDefault().getBundle(), path, false);
						final ImageData imageData = imageDescriptor.createImage().getImageData();
						queue.add(imageData);
					}
				});
				try {
					final ImageData imageData = queue.poll(10, TimeUnit.SECONDS);
					return Arrays.equals(image.getImageData().data, imageData.data);
				} catch (InterruptedException e) {
					fail("Failed to retrieve the image");
				}

				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText(iconName);

			}
		};
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldOpenEditorWhenDoubleClickOnTestElementInJUnitViewWithLinkEnabled() {
		// given
		open(new JUnitView());
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
		open(new JUnitView());
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
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		open(junitView);
		final TreeItem otherTestElement = getTreeItem("junit.lwe.TP1", "testSetStr1");
		select(otherTestElement);
		// then the selected element in the default editor should change, too
		final TextEditor defaultEditor = new TextEditor();
		assertTrue(defaultEditor.isActive());
		assertEquals("TP1.java", defaultEditor.getTitle());
		assertEquals("testSetStr1", defaultEditor.getSelectedText());
		// and in the outline view as well
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectElementInEditorWhenSelectingAnotherElementInJUnitViewWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		open(junitView);
		final TreeItem otherTestElement = getTreeItem("junit.lwe.TP1", "testSetStr1");
		select(otherTestElement);
		// then the selected element in the default editor should not have
		// changed
		final TextEditor defaultEditor = new TextEditor();
		assertTrue(defaultEditor.isActive());
		assertEquals("TP1.java", defaultEditor.getTitle());
		assertEquals("testGetStr1", defaultEditor.getSelectedText());
		// and nor in the outline view
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testGetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectElementInJUnitViewWhenSelectingAnotherElementInOutlineViewWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another element in the outline view
		open(new OutlineView());
		final TreeItem firstOutlineElement = getTreeItem("TP1", "testSetStr1");
		select(firstOutlineElement);
		// then the JUnit view selection should have changed
		activate(junitView);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestSelection = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectElementInJUnitViewWhenSelectingAnotherElementInOutlineViewWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another element in the outline view
		open(new OutlineView());
		final TreeItem firstOutlineElement = getTreeItem("TP1", "testSetStr1");
		select(firstOutlineElement);
		// then the JUnit view selection should have changed
		activate(junitView);
		assertTrue(initialTestElement.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTestElementInJUnitViewWhenSelectingAnotherMethodNameInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another method name in the editor
		final TextEditor editor = new TextEditor();
		activate(editor);
		editor.selectText("testSetStr1");
		sleep(TimePeriod.SHORT);
		// then the JUnit view selection should have changed
		activate(junitView);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestElementItem = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestElementItem.isSelected());
		// and in the outline view as well
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectTestElementInJUnitViewWhenSelectingAnotherMethodNameInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting another method name in the editor
		final TextEditor editor = new TextEditor();
		activate(editor);
		editor.selectText("testSetStr1");
		sleep(TimePeriod.SHORT);
		// then the JUnit view selection should not have changed
		activate(junitView);
		assertTrue(initialTestElement.isSelected());
		// but the outline view, yes
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTestElementInJUnitViewWhenSelectingAnotherMethodBodyElementInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting a line in another method in the editor
		final TextEditor editor = new TextEditor();
		activate(editor);
		editor.selectLine(17);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should have changed
		activate(junitView);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestSelection = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestSelection.isSelected());
		// and in the outline view as well
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectTestElementInJUnitViewWhenSelectingAnotherMethodBodyElementInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting a line in another method in the editor
		final TextEditor editor = new TextEditor();
		activate(editor);
		editor.selectLine(17);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should not have changed
		activate(junitView);
		assertTrue(initialTestElement.isSelected());
		// but the outline view, yes
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1", "testSetStr1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTestClassInJUnitViewWhenSelectingTypeNameInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting the type name in the editor
		final TextEditor editor = new TextEditor();
		activate(editor);
		editor.selectLine(6);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should have changed
		activate(junitView);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedSelection = getTreeItem("junit.lwe.TP1");
		assertTrue(expectedSelection.isSelected());
		// and in the outline view as well
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotSelectTestClassInJUnitViewWhenSelectingTypeNameInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		// when selecting the type name in the editor
		final TextEditor editor = new TextEditor();
		activate(editor);
		editor.selectLine(6);
		sleep(TimePeriod.getCustom(2));
		// then the JUnit view selection should not have changed
		activate(junitView);
		assertTrue(initialTestElement.isSelected());
		// but in the outline view, yes
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldSelectTypeElementInJUnitViewWhenSelectingAnImportStatementInEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
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
		activate(junitView);
		assertFalse(initialTestElement.isSelected());
		assertTrue(getTreeItem("junit.lwe.TP1").isSelected());
		// and in the outline view has the same selection
		open(new OutlineView());
		final TreeItem expectedOutlineSelection = getTreeItem("TP1");
		assertTrue(expectedOutlineSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotChangeSelectedElementInJUnitViewWhenSelectingAnImportStatementInEditorWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
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
		activate(junitView);
		assertTrue(initialTestElement.isSelected());
		// and in the outline view has no selection
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldActivateOtherEditorViewAndFocusWhenSelectingAnotherElementInJUnitViewWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor firstEditor = new TextEditor();
		assertTrue(firstEditor.isActive());
		assertEquals("TP1.java", firstEditor.getTitle());
		assertEquals("testGetStr1", firstEditor.getSelectedText());
		open(junitView);
		final TreeItem secondTestElement = getTreeItem("junit.lwe.TP2", "testSetStr2");
		secondTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor secondEditor = new TextEditor();
		assertTrue(secondEditor.isActive());
		assertEquals("TP2.java", secondEditor.getTitle());
		// focus is on the assertion failure in this case
		assertThat(secondEditor.getSelectedText(), containsString("assertEquals(a.getStr(), \"get\");"));
		// when selecting back the first element in the JUnit view
		activate(junitView);
		final TreeItem thirdTestElement = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		select(thirdTestElement);
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
		open(junitView);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor firstEditor = new TextEditor();
		assertTrue(firstEditor.isActive());
		assertEquals("TP1.java", firstEditor.getTitle());
		assertEquals("testGetStr1", firstEditor.getSelectedText());
		open(junitView);
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
		activate(junitView);
		final TreeItem thirdTestElement = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		select(thirdTestElement);
		// then the second editor should be active and the selection should be
		// the same as before
		final TextEditor activeEditor = new TextEditor();
		assertEquals(activeEditor.getTitle(), "TP2.java");
		assertThat(activeEditor.getSelectedText(), containsString("assertEquals(a.getStr(), \"get\");"));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenLinkAfterTestRunWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// then
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenLinkAfterTestRunWithLinkDisabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// then
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldShowBrokenLinkWhenSwitchingToNonTestClassEditorThenSyncFromOutlineWhenBackToTestClassEditorWithLinkEnabled() {
		// given
		final JUnitView junitView = new JUnitView();
		open(junitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final TreeItem initialTestElement = getTreeItem("junit.lwe.TP1", "testGetStr1");
		initialTestElement.doubleClick();
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		final TextEditor testEditor = new TextEditor();
		// when open A.java from Project Explorer
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"A.java");
		open(aClassItem);
		// then
		final TextEditor aClassEditor = new TextEditor();
		assertTrue(aClassEditor.isActive());
		assertEquals("A.java", aClassEditor.getTitle());
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNC_BROKEN_IMAGE));
		// when switch back to Test Editor
		testEditor.activate();
		sleep(TimePeriod.SHORT);
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
		// when select a method in the outline view
		open(new OutlineView());
		final TreeItem firstOutlineElement = getTreeItem("TP1", "testSetStr1");
		select(firstOutlineElement);
		// then the JUnit view selection should have changed
		activate(junitView);
		assertFalse(initialTestElement.isSelected());
		final TreeItem expectedTestSelection = getTreeItem("junit.lwe.AllTests", "junit.lwe.TP1", "testSetStr1");
		assertTrue(expectedTestSelection.isSelected());
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldShowBrokenSyncWhenTestEndsAndOtherTestClassOpenedInEditorWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNC_BROKEN_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenTestEndsAndOtherTestClassOpenedInEditorWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenTestEndsAndFirstFailingTestClassOpenedInEditorWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP2.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenTestEndsAndFirstFailingTestClassOpenedInEditorWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP2.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldShowBrokenSyncWhenTestEndsAndNonTestClassOpenedInEditorWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"A.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be in 'sync broken' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNC_BROKEN_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenTestEndsAndTestClassOpenedInEditorWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenTestEndsAndNonTestClassOpenedInEditorWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"A.java");
		open(aClassItem);
		// when running the tests again
		runAllTests();
		// then the LWE button should be in 'sync' state (because the LWE is
		// disabled)
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldShowBrokenSyncWhenOtherEditorActiveThenSyncAgainWhenTestEditorOpensWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"A.java");
		// when opening the Editor for this non-test class
		open(aClassItem);
		runAllTests();
		// then the LWE button should be in 'sync broken' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNC_BROKEN_IMAGE));
		// then, given TP1.java opened from Project Explorer
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		// when opening the Editor for this test class
		open(testClassItem);
		// then the LWE button should be in 'sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenTestClassEditorOpenedWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(testClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenTestClassEditorOpenedWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(testClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldShowBrokenSyncWhenNonTestClassEditorOpenedWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"A.java");
		open(aClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNC_BROKEN_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenNonTestClassEditorOpenedWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		activate(new JUnitView());
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"A.java");
		open(aClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenTestClassEditorOpenedAndSwitchingToProjectExplorerAndBackToJUnitViewWithLinkEnabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(aClassItem);
		activate(jUnitView);
		activate(projectExplorer);
		activate(jUnitView);

		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenTestClassEditorOpenedAndSwitchingToProjectExplorerAndBackToJUnitViewWithLinkDisabled() {
		// given JUnit view exists and A.java opened from Project Explorer
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem aClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("src", "junit.lwe",
				"TP1.java");
		open(aClassItem);
		activate(jUnitView);
		activate(projectExplorer);
		activate(jUnitView);

		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}
	
	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromProjectExplorerWithLinkEnabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromProjectExplorerWithLinkDisabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromProjectExplorerAndTestMethodSelectedInOutlineWithLinkEnabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		final OutlineView outlineView = new OutlineView();
		open(outlineView);
		final TreeItem firstOutlineElement = getTreeItem("TP3", "testSetStr3");
		select(firstOutlineElement);

		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromProjectExplorerAndTestMethodSelectedInOutlineWithLinkDisabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		final OutlineView outlineView = new OutlineView();
		open(outlineView);
		final TreeItem firstOutlineElement = getTreeItem("TP3", "testSetStr3");
		select(firstOutlineElement);

		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromJUnitViewWithLinkEnabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromJUnitViewWithLinkDisabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}

	@Test
	@LinkWithEditor(enabled = true)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromJUnitViewAndTestElementSelectedWithLinkEnabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		activate(jUnitView);
		final TreeItem testElement = getTreeItem("junit.lwe.submodule.AllTests", "junit.lwe.submodule.TP3",
				"testSetStr3");
		select(testElement);

		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertTrue(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}
		
	@Test
	@LinkWithEditor(enabled = false)
	public void shouldNotShowBrokenSyncWhenLibTestClassEditorOpenedFromJUnitViewAndTestElementSelectedWithLinkDisabled() {
		// given
		runAllNestedTests();
		final JUnitView jUnitView = new JUnitView();
		activate(jUnitView);
		final ViewToolItem viewToolItem = new ViewToolItem(LINK_WITH_EDITOR);
		// when
		final ProjectExplorer projectExplorer = new ProjectExplorer();
		activate(projectExplorer);
		final ProjectItem testClassItem = projectExplorer.getProject(TEST_PROJECT).getProjectItem("JUnit-LWE-lib.jar",
				"junit.lwe.submodule", "TP3.class");
		open(testClassItem);
		activate(jUnitView);
		final TreeItem testElement = getTreeItem("junit.lwe.submodule.AllTests", "junit.lwe.submodule.TP3",
				"testSetStr3");
		select(testElement);

		// then the LWE button should be 'in sync' state
		assertTrue(viewToolItem.isEnabled());
		assertFalse(viewToolItem.isSelected());
		assertThat(getImage(viewToolItem.getSWTWidget()), matches(SYNCED_IMAGE));
	}
	
}
