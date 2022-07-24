package io.snice.testing.runtime;

import net.sourceforge.argparse4j.inf.Namespace;

import java.util.List;

/**
 * Simple holder for the arguments as given on the command line.
 *
 * @param args      the raw original arguments as given on the command line.
 * @param namespace the recognized arguments wrapped in a {@link Namespace}
 * @param unknown   list of unknown arguments
 */
public record CliArgs(String[] args, Namespace namespace, List<String> unknown) {
}
