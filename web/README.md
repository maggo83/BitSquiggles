# BitSquiggle32 ESM package

This directory is both the source for the BitSquiggles playground and a
publishable dependency-free ESM package.

```js
import { formatHex, visualize } from "bitsquiggles";

const visual = visualize(0x12345678);
console.log(formatHex(visual.mixed));
```

The package has no runtime dependencies or build step. Its TypeScript
annotations are optional editor support; JavaScript remains the executable
source of truth. See the repository-level README and specification for the
algorithm and conformance requirements.

## License

Grug 2-Clause License. See [LICENSE](LICENSE).
