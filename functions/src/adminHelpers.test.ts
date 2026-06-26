import {describe, expect, it} from "vitest";
import {
  escapeHtml,
  extractBearerToken,
  mapPhotoReason,
  mapReason,
  mapReviewReason,
  renderAdminResponse,
} from "./adminHelpers";

describe("adminHelpers", () => {
  describe("extractBearerToken", () => {
    it("returns token from a valid Authorization header", () => {
      expect(extractBearerToken({authorization: "Bearer abc.def"})).toBe("abc.def");
    });

    it("uses the first Authorization header when multiple are provided", () => {
      expect(extractBearerToken({authorization: ["Bearer first", "Bearer second"]})).toBe("first");
    });

    it("rejects missing, malformed, and empty bearer tokens", () => {
      expect(extractBearerToken({})).toBeNull();
      expect(extractBearerToken({authorization: "Basic abc"})).toBeNull();
      expect(extractBearerToken({authorization: "Bearer   "})).toBeNull();
    });
  });

  it("escapes HTML-sensitive characters", () => {
    expect(escapeHtml("<script a=\"b\">&'</script>"))
      .toBe("&lt;script a=&quot;b&quot;&gt;&amp;&#039;&lt;/script&gt;");
  });

  it("maps known report reasons and keeps unknown reason codes visible", () => {
    expect(mapReason("DUPLICATE")).toBe("Duplikat innego miejsca [DUPLICATE]");
    expect(mapReviewReason("OFFENSIVE")).toBe("Obraźliwa treść [OFFENSIVE]");
    expect(mapPhotoReason("COPYRIGHT")).toBe("Narusza prawa autorskie [COPYRIGHT]");
    expect(mapReason("CUSTOM")).toBe("CUSTOM [CUSTOM]");
  });

  it("renders escaped admin response HTML", () => {
    const html = renderAdminResponse("<Błąd>", "Nieprawidłowe <id>");

    expect(html).toContain("kidZone Admin Panel");
    expect(html).toContain("&lt;Błąd&gt;");
    expect(html).toContain("Nieprawidłowe &lt;id&gt;");
    expect(html).not.toContain("<Błąd>");
    expect(html).not.toContain("Nieprawidłowe <id>");
  });
});
