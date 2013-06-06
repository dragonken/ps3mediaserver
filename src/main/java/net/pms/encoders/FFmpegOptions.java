package net.pms.encoders;

import net.pms.configuration.PmsConfiguration;
import net.pms.util.ProcessUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class FFmpegOptions extends OptionsHashMap {
	// ffmpeg [global_options] {[input_file_options] -i ‘input_file’} ... {[output_file_options] ‘output_file’} ...

	private static final long serialVersionUID = -1283795835781170081L;

	// options that go in the 'global_options' slot
	public static final List<String> globals = Arrays.asList(
		// global options:
		"-loglevel", "-v", "-report", "-max_alloc", "-y", "-n", "-stats",
		"-bits_per_raw_sample", "-croptop", "-cropbottom", "-cropleft", "-cropright",
		"-padtop", "-padbottom", "-padleft", "-padright", "-padcolor", "-vol",
		// Advanced global options:
		"-cpuflags", "-benchmark", "-benchmark_all", "-progress", "-stdin", "-timelimit",
		"-dump", "-hex", "-vsync", "-async", "-adrift_threshold", "-copyts", "-copytb",
		"-dts_delta_threshold", "-dts_error_threshold", "-xerror", "-filter_complex",
		"-debug_ts", "-intra", "-vdt", "-sameq", "-same_quant", "-deinterlace", "-psnr",
		"-vstats", "-vstats_file", "-dc", "-qphist", "-vc", "-tvstd", "-isync"
	);

	// options that go in the 'input_file_options' slot
	public static final List<String> input_file_options = Arrays.asList(
		// http options
		"-seekable", "-chunked_post", "-headers", "-content_type", "-user-agent",
		"-multiple_requests", "-post_data", "-timeout", "-mime_type", "-cookies",
		// rtmp_options
		"-rtmp_app", "-rtmp_playpath",
		// srtp_options
		"-srtp_out_suite", "-srtp_out_params", "-srtp_in_suite", "-srtp_in_params",
		// tcp_options
		"-listen", "-timeout", "-listen_timeout",
		// udp_options
		"-buffer_size", "-localport", "-localaddr", "-pkt_size", "-reuse", "-ttl",
		"-connect", "-fifo_size", "-overrun_nonfatal", "-timeout",
		// crypto_options
		"-key", "-iv",
		// file_options
		"-truncate",
		// bluray_options
		"-playlist", "-angle", "-chapter",
		// input-only options
		"-itsoffset", "-dump_attachment", "-guess_layout_max", "-re", "-loop_input",
		"-muxdelay", "-muxpreload"
	);

	// Since we're primarily concerned with configuring the output stream
	// we consider everything else to be destined for the 'output_file_options'
	// slot, though in reality many of the remaining options can be used
	// with input files too.
	public void transferGlobals(List<String> list) {
		transferAny(globals, list);
	}

	public void transferInputFileOptions(List<String> list) {
		// check for CRLF in header fields
		if (containsKey("-headers")) {
			String headers = get("-headers");
			// if it already has CRLF we assume it's ok
			if (!headers.contains("\r\n")) {
				// otherwise we try to fix it
				headers = headers.replace("User-Agent: ", "\r\nUser-Agent: ")
					.replace("Cookie: ", "\r\nCookie: ")
					.replace("Referer: ", "\r\nReferer: ")
					.replace("Accept: ", "\r\nAccept: ")
					.replace("Range: ", "\r\nRange: ")
					.replace("Connection: ", "\r\nConnection: ")
					.replace("Content-Length: ", "\r\nContent-Length: ")
					.replace("Content-Type: ", "\r\nContent-Type: ")
					.trim() + "\r\n";
				put("-headers", headers);
			}
		}

		transferAny(input_file_options, list);
	}

	public FFmpegOptions() { }

	public static List<String> getSupportedProtocols(PmsConfiguration configuration) {
		ArrayList<String> protocols = new ArrayList<String>();
		String output = ProcessUtil.run(configuration.getFfmpegPath(), "-protocols");
		boolean add = false;

		for (String line : output.split("\n")) {
			if (line.equals("Input:")) {
				add = true;
			} else if (line.equals("Output:")) {
				break;
			} else if (add) {
				protocols.add(line);
			}
		}

		return protocols;
	}
}

// A HashMap of options and args (if any)
// which preserves insertion order
class OptionsHashMap extends LinkedHashMap<String, String> {
	private static final long serialVersionUID = 7021453139296691483L;

	public void addAll(List<String> args) {
		String opt = null, optarg = null;
		args.add("-NULL");

		for (String arg : args) {
			if (arg.startsWith("-")) {
				// flush
				if (opt != null) {
					put(opt, optarg);
				}
				opt = arg;
				optarg = null;
			} else {
				optarg = arg;
			}
		}

		args.remove("-NULL");
	}

	public void transfer(String opt, List<String> list) {
		if (containsKey(opt)) {
			_transfer(opt, list);
		}
	}

	public void transferAny(List<String> opts, List<String> list) {
		for (String opt : opts) {
			transfer(opt, list);
		}
	}

	public void transferAll(List<String> list) {
		for (Object opt : keySet().toArray()) {
			_transfer((String) opt, list);
		}
	}

	private void _transfer(String opt, List<String> list) {
		list.add(opt);
		String optarg = remove(opt);
		if (optarg != null) {
			list.add(optarg);
		}
	}
}
