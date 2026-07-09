// Serves /sitemap.xml — a plain XML list of the site's pages for search engines.
// The "[.]" in the filename is how the file-based router writes a literal dot,
// so this file maps to the /sitemap.xml path. No React page, just an XML response.

import { createFileRoute } from "@tanstack/react-router";
import type {} from "@tanstack/react-start";

// Change this to the deployed site URL when you have one.
const BASE_URL = "https://algorythm.example.com";

// The real pages of the app.
const PAGES = ["/", "/library", "/syntax"];

export const Route = createFileRoute("/sitemap.xml")({
  server: {
    handlers: {
      GET: async () => {
        const urls = PAGES.map((path) => `  <url><loc>${BASE_URL}${path}</loc></url>`).join("\n");

        const xml = [
          `<?xml version="1.0" encoding="UTF-8"?>`,
          `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">`,
          urls,
          `</urlset>`,
        ].join("\n");

        return new Response(xml, {
          headers: {
            "Content-Type": "application/xml",
            "Cache-Control": "public, max-age=3600",
          },
        });
      },
    },
  },
});
