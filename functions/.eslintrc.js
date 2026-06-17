module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
    "plugin:import/errors",
    "plugin:import/warnings",
    "plugin:import/typescript",
    "google",
    "plugin:@typescript-eslint/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    project: ["tsconfig.json", "tsconfig.dev.json"],
    sourceType: "module",
  },
  ignorePatterns: [
    "/lib/**/*",
    "/generated/**/*",
  ],
  plugins: [
    "@typescript-eslint",
    "import",
  ],
  rules: {
    // Relaxed from Google style defaults
    "max-len": ["warn", {code: 120, ignoreUrls: true, ignoreStrings: true, ignoreTemplateLiterals: true}],
    "quotes": ["warn", "double"],
    "import/no-unresolved": 0,
    "indent": ["error", 2, {SwitchCase: 1}],
    "require-jsdoc": "off",
    "valid-jsdoc": "off",
    "no-multiple-empty-lines": ["warn", {max: 2}],
    "object-curly-spacing": ["warn", "never"],
    "operator-linebreak": "off",
    "linebreak-style": "off",
    "comma-dangle": ["warn", "always-multiline"],
    "@typescript-eslint/no-explicit-any": "warn",
  },
};
