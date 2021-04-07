package com.ms.msde.stash.hook.alias;

import com.atlassian.bitbucket.scm.CommandOutputHandler;
import com.atlassian.utils.process.StringOutputHandler;

class StringCommandOutputHandler extends StringOutputHandler implements CommandOutputHandler<String> {
}
