import { Link } from "react-router-dom";
import { Card } from "@/components/ui/card";

type ClickableCardProps = {
  to: string;
  children: React.ReactNode;
  className?: string;
};

export default function ClickableCard({
  to,
  children,
  className,
}: ClickableCardProps) {
  return (
    <Link to={to} className="block">
      <Card
        className={`rounded-2xl hover:-translate-y-0.5 hover:shadow-md transition-transform transition-shadow ${className || ""}`}
      >
        {children}
      </Card>
    </Link>
  );
}
