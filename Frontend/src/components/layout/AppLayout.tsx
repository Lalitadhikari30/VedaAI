import { useState } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';

interface AppLayoutProps {
  children: React.ReactNode;
  collapsedSidebar?: boolean;
  onToggleSidebar?: () => void;
}

export default function AppLayout({
  children,
  collapsedSidebar = true,
  onToggleSidebar,
}: AppLayoutProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(collapsedSidebar);

  const handleToggle = () => {
    if (onToggleSidebar) {
      onToggleSidebar();
    } else {
      setIsCollapsed((prev) => !prev);
    }
  };

  return (
    <div className="min-h-screen flex bg-[#F4F5F8] text-slate-800 antialiased selection:bg-orange-100 selection:text-orange-600">
      {/* Dark Icon Sidebar - Desktop */}
      <Sidebar
        collapsed={isCollapsed}
        onToggle={handleToggle}
      />

      {/* Mobile Drawer Backdrop */}
      {mobileMenuOpen && (
        <div 
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
        />
      )}

      {/* Main Content Column */}
      <div className="flex-1 flex flex-col min-w-0 min-h-screen bg-[#F4F5F8]">
        {/* Top Header */}
        <Header
          onMenuToggle={() => setMobileMenuOpen((prev) => !prev)}
        />

        {/* Full-width Main Content */}
        <main className="flex-1 flex flex-col w-full min-w-0 overflow-hidden">
          {children}
        </main>
      </div>
    </div>
  );
}
