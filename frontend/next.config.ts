import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [{
      protocol: "https",
      hostname: "shared.fastly.steamstatic.com",
      pathname: "/store_item_assets/steam/apps/*/header.jpg",
      search: "",
    }],
  },
};

export default nextConfig;

