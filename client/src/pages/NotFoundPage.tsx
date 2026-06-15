// Fallback for unknown URLs.

import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold">404 — Page not found</h1>
      <Link to="/" className="text-muted-foreground underline">
        Go back home
      </Link>
    </div>
  );
}
