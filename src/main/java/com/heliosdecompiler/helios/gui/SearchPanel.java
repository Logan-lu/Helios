package com.heliosdecompiler.helios.gui;

import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.LoadedFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class SearchPanel {
    private Composite mainComposite;

    private Composite accessComposite;
    private Composite stringComposite;

    private Text owner;
    private Text name;
    private Text desc;

    private Text string;
    private Button regex;

    private Tree outputTree;

    private int selection;

    private Button searchButton;

    SearchPanel(SashForm sashForm) {
        Composite comp = new Composite(sashForm, SWT.BORDER);
        comp.setLayout(new GridLayout(1, true));
        {
            Combo dropdown = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
            dropdown.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            dropdown.add("Method Access");
            dropdown.add("Field Access");
            dropdown.add("String constant");
            dropdown.select(0);
            dropdown.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    selection = dropdown.getSelectionIndex();
                    if (selection == 0 || selection == 1) {
                        ((GridData) stringComposite.getLayoutData()).exclude = true;
                        ((GridData) accessComposite.getLayoutData()).exclude = false;
                        stringComposite.setVisible(false);
                        accessComposite.setVisible(true);
                    } else {
                        ((GridData) stringComposite.getLayoutData()).exclude = false;
                        ((GridData) accessComposite.getLayoutData()).exclude = true;
                        stringComposite.setVisible(true);
                        accessComposite.setVisible(false);
                    }
                    mainComposite.layout();
                }
            });
        }
        mainComposite = new Composite(comp, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, true));
        mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        searchButton = new Button(mainComposite, SWT.NONE);
        searchButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        searchButton.setText("Search");
        searchButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (selection == 0) {
                    String o = owner.getText();
                    String n = name.getText();
                    String d = desc.getText();
                    Helios.submitBackgroundTask(() -> {
                        for (ClassNode classNode : Helios.loadAllClasses()) {
                            for (MethodNode m : classNode.methods) {
                                InsnList insnList = m.instructions;
                                for (AbstractInsnNode abstractInsnNode : insnList.toArray()) {
                                    if (abstractInsnNode instanceof MethodInsnNode) {
                                        MethodInsnNode min = (MethodInsnNode) abstractInsnNode;
                                        if (min.owner.equals(o) && min.name.equals(n) && min.desc.equals(d)) {
                                            System.out.println(classNode.name + " " + m.name + " " + m.desc);
                                        }
                                    }
                                }
                            }
                        }
                    });
                } else if (selection == 1) {
                    String o = owner.getText();
                    String n = name.getText();
                    String d = desc.getText();
                    Helios.submitBackgroundTask(() -> {
                        for (ClassNode classNode : Helios.loadAllClasses()) {
                            for (MethodNode m : classNode.methods) {
                                InsnList insnList = m.instructions;
                                for (AbstractInsnNode abstractInsnNode : insnList.toArray()) {
                                    if (abstractInsnNode instanceof FieldInsnNode) {
                                        FieldInsnNode fin = (FieldInsnNode) abstractInsnNode;
                                        if (fin.owner.equals(o) && fin.name.equals(n) && fin.desc.equals(d)) {
                                            System.out.println(classNode.name + " " + m.name + " " + m.desc);
                                        }
                                    }
                                }
                            }
                        }
                    });
                } else {
                    String lookup = string.getText().toLowerCase();
                    boolean usePattern = regex.getSelection();
                    Helios.submitBackgroundTask(() -> {
                        class Result {
                            public ClassNode classNode;
                            public FieldNode fieldNode;
                            public MethodNode methodNode;
                            public String value;

                            public Result(ClassNode classNode, FieldNode fieldNode, MethodNode methodNode, String value) {
                                this.classNode = classNode;
                                this.fieldNode = fieldNode;
                                this.methodNode = methodNode;
                                this.value = value;
                            }
                        }

                        Map<LoadedFile, List<Result>> resultByFile = new HashMap<>();

                        Pattern p = Pattern.compile(lookup);
                        Predicate<String> matches = (str) -> usePattern ? p.matcher(str).find() : str.toLowerCase().contains(lookup);

                        for (LoadedFile loadedFile : Helios.getAllFiles()) {
                            loadedFile.getAllData().keySet().forEach(loadedFile::getClassNode);
                            List<Result> results = new ArrayList<>();
                            for (ClassNode classNode : loadedFile.getAllClassNodes()) {
                                for (FieldNode fieldNode : classNode.fields) {
                                    Object v = fieldNode.value;
                                    if (v instanceof String) {
                                        String s = (String) v;
                                        if (!s.isEmpty()) {
                                            if (matches.test(s)) {
                                                results.add(new Result(classNode, fieldNode, null, s));
                                            }
                                        }
                                    }
                                }
                                for (MethodNode m : classNode.methods) {
                                    InsnList insnList = m.instructions;
                                    for (AbstractInsnNode abstractInsnNode : insnList.toArray()) {
                                        if (abstractInsnNode instanceof LdcInsnNode) {
                                            if (((LdcInsnNode) abstractInsnNode).cst instanceof String) {
                                                final String s = (String) ((LdcInsnNode) abstractInsnNode).cst;
                                                if (!s.isEmpty()) {
                                                    if (matches.test(s)) {
                                                        results.add(new Result(classNode, null, m, s));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            resultByFile.put(loadedFile, results);
                        }

                        List<TempTreeItem> roots = new ArrayList<>();

                        for (Map.Entry<LoadedFile, List<Result>> ent : resultByFile.entrySet()) {
                            Map<String, TempTreeItem> map = new HashMap<>();
                            TempTreeItem root = new TempTreeItem();
                            root.name = ent.getKey().getName();
                            for (Result r : ent.getValue()) {
                                final String[] spl = r.classNode.name.split("/");
                                TempTreeItem last = root;
                                for (int i = 0; i < spl.length; i++) {
                                    String joined = join(i, spl);
                                    TempTreeItem child = map.get(joined);
                                    if (child == null) {
                                        child = new TempTreeItem();
                                        child.parent = last;
                                        child.name = spl[i];
                                        last.children.add(child);
                                        map.put(joined, child);
                                    }
                                    last = child;
                                }
                                TempTreeItem val = new TempTreeItem();
                                last.children.add(val);
                                val.parent = last;
                                val.name = r.fieldNode == null ? r.methodNode.name + r.methodNode.desc + " -> " + r.value : r.fieldNode.name + " " + r.fieldNode.desc + " -> " + r.value;
                            }
                            roots.add(root);
                        }

                        sort(roots);

                        mainComposite.getDisplay().syncExec(() -> {
                            for (TreeItem item : outputTree.getItems()) {
                                item.setExpanded(false);
                                item.dispose();
                            }
                            try {
                                outputTree.setRedraw(false);
                                for (TempTreeItem root : roots) {
                                    update(new TreeItem(outputTree, SWT.NONE), root);
                                }
                            } finally {
                                outputTree.setRedraw(true);
                            }
                        });
                    });
                }
            }
        });
        createAccessComposite(0);
        createStringComposite();
        createOutput();
        mainComposite.layout();
    }

    class TempTreeItem {
        String name;
        final List<TempTreeItem> children = new ArrayList<>();
        TempTreeItem parent;
    }

    private String join(int end, String[] arr) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= end; i++) {
            out.append(arr[i]).append('/');
        }
        if (out.length() > 0 && out.charAt(out.length() - 1) == '/') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    private void update(TreeItem last, TempTreeItem lastspoof) {
        while (last.getDisplay().readAndDispatch()) ;
        last.setText(lastspoof.name);
        for (TempTreeItem child : lastspoof.children) {
            update(new TreeItem(last, SWT.NONE), child);
        }
    }

    private void sort(List<TempTreeItem> items) {
        Collections.sort(items, (o1, o2) -> {
            return o1.name.compareTo(o2.name);
        });
        for (TempTreeItem spoof : items) {
            sort(spoof.children);
        }
    }

    private void createAccessComposite(int id) {
        accessComposite = new Composite(mainComposite, SWT.NONE);
        accessComposite.setLayout(new GridLayout(1, true));
        accessComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        accessComposite.moveAbove(searchButton);
        {
            Composite classNameComposite = new Composite(accessComposite, SWT.NONE);
            classNameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            FillLayout classNameLayout = new FillLayout();
            classNameLayout.type = SWT.HORIZONTAL;
            classNameComposite.setLayout(classNameLayout);
            Label ownerLabel = new Label(classNameComposite, SWT.NONE);
            ownerLabel.setText("Class Name");
            owner = new Text(classNameComposite, SWT.BORDER);
        }
        {
            Composite classNameComposite = new Composite(accessComposite, SWT.NONE);
            classNameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            FillLayout classNameLayout = new FillLayout();
            classNameLayout.type = SWT.HORIZONTAL;
            classNameComposite.setLayout(classNameLayout);
            Label nameLabel = new Label(classNameComposite, SWT.NONE);
            nameLabel.setText((id == 0 ? "Method" : "Field") + " Name");
            name = new Text(classNameComposite, SWT.BORDER);
        }
        {
            Composite classNameComposite = new Composite(accessComposite, SWT.NONE);
            classNameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            FillLayout classNameLayout = new FillLayout();
            classNameLayout.type = SWT.HORIZONTAL;
            classNameComposite.setLayout(classNameLayout);
            Label descLabel = new Label(classNameComposite, SWT.NONE);
            descLabel.setText((id == 0 ? "Method" : "Field") + " Desc");
            desc = new Text(classNameComposite, SWT.BORDER);
        }
    }

    private void createStringComposite() {
        stringComposite = new Composite(mainComposite, SWT.NONE);
        stringComposite.setLayout(new GridLayout(1, true));
        stringComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        stringComposite.moveAbove(searchButton);
        {
            Composite classNameComposite = new Composite(stringComposite, SWT.NONE);
            classNameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            FillLayout classNameLayout = new FillLayout();
            classNameLayout.type = SWT.HORIZONTAL;
            classNameComposite.setLayout(classNameLayout);
            Label stringLabel = new Label(classNameComposite, SWT.NONE);
            stringLabel.setText("Text");
            string = new Text(classNameComposite, SWT.BORDER);
        }
        {
            Composite classNameComposite = new Composite(stringComposite, SWT.NONE);
            classNameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            FillLayout classNameLayout = new FillLayout();
            classNameLayout.type = SWT.HORIZONTAL;
            classNameComposite.setLayout(classNameLayout);
            Label regexLabel = new Label(classNameComposite, SWT.NONE);
            regexLabel.setText("Regex");
            regex = new Button(classNameComposite, SWT.CHECK);
        }
        ((GridData) stringComposite.getLayoutData()).exclude = true;
    }

    private void createOutput() {
        outputTree = new Tree(mainComposite, SWT.BORDER);
        outputTree.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
}
