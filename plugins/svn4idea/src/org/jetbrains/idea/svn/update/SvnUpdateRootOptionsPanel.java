/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.svn.update;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.versionBrowser.RepositoryVersion;
import com.intellij.openapi.project.Project;
import org.jetbrains.idea.svn.SvnBundle;
import org.jetbrains.idea.svn.SvnConfiguration;
import org.jetbrains.idea.svn.SvnVcs;
import org.jetbrains.idea.svn.history.SvnVersionsProvider;
import org.jetbrains.idea.svn.dialogs.SelectLocationDialog;
import org.tmatesoft.svn.core.wc.SVNRevision;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SvnUpdateRootOptionsPanel implements SvnPanel{
  private TextFieldWithBrowseButton myURLText;
  private JLabel myURLLabel;
  private JCheckBox myRevisionBox;
  private TextFieldWithBrowseButton myRevisionText;

  private final SvnVcs myVcs;
  private JPanel myPanel;
  private FilePath myRoot;

  public SvnUpdateRootOptionsPanel(FilePath root, final SvnVcs vcs) {

    myRoot = root;
    myVcs = vcs;

    myURLText.setEditable(false);
    myURLLabel.setLabelFor(myURLText);


    myRevisionBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (e.getSource() == myRevisionBox) {
          myRevisionText.setEnabled(myRevisionBox.isSelected());
          if (myRevisionBox.isSelected()) {
            myRevisionText.getTextField().selectAll();
            myRevisionText.requestFocus();
          }
        }
        else {
          SelectLocationDialog dialog = new SelectLocationDialog(myVcs.getProject(), myURLText.getText());
          dialog.show();
          if (dialog.isOK()) {
            String selected = dialog.getSelectedURL();
            if (selected != null) {
              myURLText.setText(selected);
            }
          }
        }
      }
    });

    myRevisionText.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final Project project = vcs.getProject();
        final RepositoryVersion repositoryVersion =
          AbstractVcsHelper.getInstance(project).chooseRepositoryVersion(new SvnVersionsProvider(project, myURLText.getText()));
        if (repositoryVersion != null) {
          myRevisionText.setText(String.valueOf(repositoryVersion.getNumber()));
        }
      }
    });

    myRevisionText.setText(SVNRevision.HEAD.toString());
    myRevisionText.getTextField().selectAll();
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public void reset(final SvnConfiguration configuration) {
    final UpdateRootInfo rootInfo = configuration.getUpdateRootInfo(myRoot.getIOFile(), myVcs);
    myURLText.setText(rootInfo.getUrl().toString());
    myRevisionBox.setSelected(rootInfo.isUpdateToRevision());
    myRevisionText.setText(rootInfo.getRevision().getName());
  }

  public void apply(final SvnConfiguration configuration) throws ConfigurationException {
    final UpdateRootInfo rootInfo = configuration.getUpdateRootInfo(myRoot.getIOFile(), myVcs);
    rootInfo.setUrl(myURLText.getText());
    rootInfo.setUpdateToRevision(myRevisionBox.isSelected());
    final SVNRevision revision = SVNRevision.parse(myRevisionText.getText());
    if (!revision.isValid()) {
      throw new ConfigurationException(SvnBundle.message("invalid.svn.revision.error.message", myRevisionText.getText()));
    }
    rootInfo.setRevision(revision);
  }
}
