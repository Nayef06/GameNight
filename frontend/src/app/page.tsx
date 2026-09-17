import Link from "next/link";

export default function Home() {
  return (
    <section className="card hero">
      <h1>GameNight</h1>
      <p>Find the games everyone in your group owns.</p>
      <div className="actions">
        <Link className="button" href="/login">Login</Link>
        <Link className="button secondary" href="/register">Create Account</Link>
      </div>
    </section>
  );
}
